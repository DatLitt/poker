package com.example.poker.game;

import com.example.poker.model.*;
import com.example.poker.service.TableManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameEngine {

    private GameState state = GameState.WAITING;

    private Deck deck;

    private final TableManager table;
    private final Runnable onGameEnd;

    private final ObjectMapper mapper = new ObjectMapper();

    private final List<String> community = new ArrayList<>();

    private int currentTurn = -1;

    private int pot = 0;

    private int currentBet = 0;

    private final int smallBlind = 10;

    private final int bigBlind = 20;

    private final int startingChips = 1000;

    private int dealerSeat = 0;

    private int minRaiseAmount = 0;

    private final ScheduledExecutorService endGameScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final int endGamePauseSeconds = 10;

    private volatile boolean endPauseActive = false;

    public GameEngine(TableManager table, Runnable onGameEnd) {
        this.table = table;
        this.onGameEnd = onGameEnd;
    }

    private List<Integer> buildMoneySnapshot() {
        List<Integer> money = new ArrayList<>();
        for (Player p : table.getPlayers()) {
            money.add(p == null ? null : p.getChips());
        }
        return money;
    }

    private void awardPotToWinners(List<Player> winners) {
        if (winners == null || winners.isEmpty() || pot <= 0) return;

        int share = pot / winners.size();
        int remainder = pot % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            Player winner = winners.get(i);
            if (winner == null) continue;
            int payout = share + (i == 0 ? remainder : 0);
            winner.setChips(winner.getChips() + payout);
        }

        pot = 0;
    }

    // ---------- GAME START ----------
    public void startGame() throws Exception {

        if (table.getPlayerCount() < 2) {
            state = GameState.WAITING;
            return;
        }

        state = GameState.PREFLOP;

        deck = new Deck();

        community.clear();

        for (Player p : table.getPlayers()) {

            if (p == null) continue;

            p.resetForRound();

            p.getCards().clear();

            p.getCards().add(deck.draw());
            p.getCards().add(deck.draw());

            Map<String, Object> msg = new HashMap<>();

            msg.put("type", "deal_cards");
            msg.put("cards", p.getCards());

            if (p.getSession().isOpen()) {
                p.getSession().sendMessage(
                        new TextMessage(mapper.writeValueAsString(msg))
                );
            }
        }

        // rotate dealer button
        dealerSeat = (dealerSeat + 1) % table.getPlayers().size();
        
        // find next active player for small blind
        int sbSeat = findNextSeat(dealerSeat);
        int bbSeat = findNextSeat(sbSeat);
        
        // post blinds (allow short stacks to post all-in)
        int sbPosted = 0;
        if (sbSeat >= 0) {
            Player sb = table.getPlayers().get(sbSeat);
            if (sb != null && sb.getChips() > 0) {
                sbPosted = Math.min(smallBlind, sb.getChips());
                sb.setChips(sb.getChips() - sbPosted);
                sb.setCurrentBet(sbPosted);
                if (sb.getChips() == 0) {
                    sb.setAllIn(true);
                }
                pot += sbPosted;
            }
        }
        int bbPosted = 0;
        if (bbSeat >= 0) {
            Player bb = table.getPlayers().get(bbSeat);
            if (bb != null && bb.getChips() > 0) {
                bbPosted = Math.min(bigBlind, bb.getChips());
                bb.setChips(bb.getChips() - bbPosted);
                bb.setCurrentBet(bbPosted);
                if (bb.getChips() == 0) {
                    bb.setAllIn(true);
                }
                pot += bbPosted;
            }
        }
        currentBet = Math.max(sbPosted, bbPosted);
        minRaiseAmount = currentBet > 0 ? currentBet : bigBlind;

        startBettingRound();
    }

    // ---------- BETTING ROUND ----------
    private void startBettingRound() throws Exception {

        if (!hasActivePlayerWhoCanAct()) {
            fastForwardToShowdown();
            return;
        }

        for (Player p : table.getPlayers()) {
            if (p == null || p.isFolded()) continue;
            if (p.isAllIn() || p.getChips() <= 0) {
                p.setActed(true);
            } else {
                p.setActed(false);
            }
        }

        // On PREFLOP: action starts after big blind
        // On other streets: action starts after dealer (small blind position)
        if (state == GameState.PREFLOP) {
            currentTurn = findNextSeat(findNextSeat(dealerSeat)); // dealer → SB → BB → UTG(current)
        } else {
            currentTurn = findNextSeat(dealerSeat); // dealer → SB(current)
        }

        if (currentTurn == -1) {
            nextStage();
            return;
        }

        sendTurn();
    }

    private int findNextSeat(int fromSeat) {

        List<Player> players = table.getPlayers();

        int size = players.size();

        for (int i = 1; i <= size; i++) {

            int seat = (fromSeat + i) % size;

            Player p = players.get(seat);

            if (p == null) continue;
            if (p.isFolded()) continue;
            if (p.isAllIn()) continue;
            if (p.getChips() <= 0) continue;

            return seat;
        }

        return -1;
    }

    private void sendTurn() throws Exception {

        Map<String, Object> msg = new HashMap<>();

        msg.put("type", "player_turn");
        msg.put("seat", currentTurn);
        
        // determine allowed actions
        Player currentPlayer = table.getPlayers().get(currentTurn);
        List<String> actions = new ArrayList<>();
        actions.add("fold");
        
        if (currentBet > currentPlayer.getCurrentBet()) {
            actions.add("call");
        } else {
            actions.add("check");
        }
        
        if (currentPlayer.getChips() > 0) {
            actions.add("raise");
            actions.add("all_in");
        }
        
        msg.put("allowedActions", actions);
        msg.put("pot", pot);
        msg.put("currentBet", currentBet);
        msg.put("dealerSeat", dealerSeat);

        broadcast(msg);
    }

    // ---------- PLAYER ACTION ----------
    public void handleAction(int seat, String action, Integer amount) throws Exception {

        if (state == GameState.WAITING || state == GameState.SHOWDOWN) return;
        if (seat != currentTurn) return;

        Player p = table.getPlayers().get(seat);
        if (p == null || p.isFolded() || p.isAllIn() || p.getChips() <= 0) return;

        switch (action) {
            case "fold":
                p.setFolded(true);
                p.setActed(true);
                break;

            case "check":
                // only valid if no bet to call
                if (currentBet > p.getCurrentBet()) return;
                p.setActed(true);
                break;

            case "call":
                int callAmount = currentBet - p.getCurrentBet();
                if (callAmount < 0) callAmount = 0;
                if (callAmount > p.getChips()) {
                    // all in
                    pot += p.getChips();
                    p.setCurrentBet(p.getCurrentBet() + p.getChips());
                    p.setChips(0);
                    p.setAllIn(true);
                } else {
                    pot += callAmount;
                    p.setChips(p.getChips() - callAmount);
                    p.setCurrentBet(currentBet);
                }
                p.setActed(true);
                break;

            case "raise":
                if (amount == null || amount <= 0) return;
                // treat amount as raise size (increment over current bet)
                int raiseSize = amount;
                if (raiseSize < minRaiseAmount) return;

                int newBet = currentBet + raiseSize;
                int totalToPut = newBet - p.getCurrentBet();
                if (totalToPut <= 0 || totalToPut > p.getChips()) return;

                pot += totalToPut;
                p.setChips(p.getChips() - totalToPut);
                if (p.getChips() == 0) {
                    p.setAllIn(true);
                }
                p.setCurrentBet(newBet);
                minRaiseAmount = raiseSize;
                currentBet = newBet;
                
                // reset acted for others
                for (Player other : table.getPlayers()) {
                    if (other != null && !other.isFolded() && !other.isAllIn() && other != p) {
                        other.setActed(false);
                    }
                }
                p.setActed(true);
                break;

            case "all_in":
                if (p.getChips() == 0) return;
                int allInAmount = p.getChips();
                int allInTotal = p.getCurrentBet() + allInAmount;
                
                pot += allInAmount;
                p.setChips(0);
                p.setAllIn(true);
                p.setCurrentBet(allInTotal);
                
                // if all-in is more than current bet, it counts as a raise
                if (allInTotal > currentBet) {
                    minRaiseAmount = allInTotal - currentBet;
                    currentBet = allInTotal;
                    // reset acted for others
                    for (Player other : table.getPlayers()) {
                        if (other != null && !other.isFolded() && !other.isAllIn() && other != p) {
                            other.setActed(false);
                        }
                    }
                }
                p.setActed(true);
                break;
        }

        if (checkWin()) {
            return;
        }
        if (isRoundComplete()) {
            nextStage();
            return;
        }
        nextTurn();
    }

    // ---------- TURN ----------
    private void nextTurn() throws Exception {

        currentTurn = findNextSeat(currentTurn);

        if (currentTurn == -1) {
            if (isRoundComplete()) {
                nextStage();
            } else {
                startBettingRound();
            }
            return;
        }

        sendTurn();
    }

    // ---------- STAGE ----------
    private void nextStage() throws Exception {

        if (state == GameState.PREFLOP) {

            state = GameState.FLOP;
            currentBet = 0;
            resetBetsForNewStreet();

            community.add(deck.draw());
            community.add(deck.draw());
            community.add(deck.draw());

        } else if (state == GameState.FLOP) {

            state = GameState.TURN;
            currentBet = 0;
            resetBetsForNewStreet();

            community.add(deck.draw());

        } else if (state == GameState.TURN) {

            state = GameState.RIVER;
            currentBet = 0;
            resetBetsForNewStreet();

            community.add(deck.draw());

        } else {

            showdown();
            return;
        }

        currentBet = 0;
        minRaiseAmount = bigBlind;
        broadcastCommunity();

        startBettingRound();
    }

    private void resetBetsForNewStreet() {
        for (Player p : table.getPlayers()) {
            if (p != null) {
                p.setCurrentBet(0);
            }
        }
    }

    private boolean hasActivePlayerWhoCanAct() {
        for (Player p : table.getPlayers()) {
            if (p == null || p.isFolded()) continue;
            if (!p.isAllIn() && p.getChips() > 0) {
                return true;
            }
        }
        return false;
    }

    private void fastForwardToShowdown() throws Exception {
        while (state != GameState.RIVER) {
            if (state == GameState.PREFLOP) {
                state = GameState.FLOP;
                community.add(deck.draw());
                community.add(deck.draw());
                community.add(deck.draw());
            } else if (state == GameState.FLOP) {
                state = GameState.TURN;
                community.add(deck.draw());
            } else if (state == GameState.TURN) {
                state = GameState.RIVER;
                community.add(deck.draw());
            } else {
                break;
            }

            currentBet = 0;
            minRaiseAmount = bigBlind;
            broadcastCommunity();
        }

        showdown();
    }

    private boolean isRoundComplete() {
        for (Player p : table.getPlayers()) {
            if (p == null || p.isFolded()) continue;
            if (!p.hasActed()) return false;
            if (p.getCurrentBet() != currentBet && !p.isAllIn()) return false;
        }
        return true;
    }

    // ---------- COMMUNITY ----------
    private void broadcastCommunity() throws Exception {

        Map<String, Object> msg = new HashMap<>();

        msg.put("type", "community_cards");
        msg.put("stage", state.name());
        msg.put("cards", community);
        msg.put("pot", pot);
        msg.put("currentBet", currentBet);

        broadcast(msg);
    }

    // ---------- WIN CHECK ----------
    private boolean checkWin() throws Exception {

        List<Player> active = new ArrayList<>();

        for (Player p : table.getPlayers()) {
            if (p != null && !p.isFolded())
                active.add(p);
        }

        if (active.size() <= 1) {
            Player winner = active.size()==1 ? active.get(0) : null;

            if (winner != null) {
                awardPotToWinners(Collections.singletonList(winner));
            }

            Map<String,Object> msg = new HashMap<>();
            msg.put("type","game_win");
            msg.put("seat", winner != null ? winner.getSeat() : -1);

            broadcast(msg);

            Map<String, Object> showdownMsg = new HashMap<>();
            showdownMsg.put("type", "showdown");
            showdownMsg.put("winnerSeat", winner != null ? winner.getSeat() : -1);
            List<Integer> winnerSeats = new ArrayList<>();
            if (winner != null) {
                winnerSeats.add(winner.getSeat());
            }
            showdownMsg.put("winnerSeats", winnerSeats);
            showdownMsg.put("community", community);

            List<Map<String, Object>> winnerBestHands = new ArrayList<>();
            if (winner != null && winner.getCards().size() + community.size() >= 5) {
                Map<String, Object> hand = new HashMap<>();
                hand.put("seat", winner.getSeat());
                hand.put("cards", HandEvaluator.bestFiveCards(winner.getCards(), community));
                winnerBestHands.add(hand);
            }
            showdownMsg.put("winnerBestHands", winnerBestHands);

            broadcast(showdownMsg);

            finishGame();
            return true;
        }
        return false;
    }

    // ---------- SHOWDOWN ----------
    private void showdown() throws Exception {

        List<Player> active = new ArrayList<>();
        for (Player p : table.getPlayers()) {
            if (p != null && !p.isFolded()) {
                active.add(p);
            }
        }

        List<Player> winners = active.isEmpty()
                ? new ArrayList<>()
                : HandEvaluator.determineWinners(active, community);
        Player winner = winners.isEmpty() ? null : winners.get(0);

        awardPotToWinners(winners);

        Map<String, Object> msg = new HashMap<>();

        msg.put("type", "showdown");
        msg.put("winnerSeat", winner != null ? winner.getSeat() : -1);
        List<Integer> winnerSeats = new ArrayList<>();
        for (Player p : winners) {
            winnerSeats.add(p.getSeat());
        }
        msg.put("winnerSeats", winnerSeats);
        msg.put("community", community);

        List<Map<String, Object>> winnerBestHands = new ArrayList<>();
        for (Player p : winners) {
            Map<String, Object> hand = new HashMap<>();
            hand.put("seat", p.getSeat());
            hand.put("cards", HandEvaluator.bestFiveCards(p.getCards(), community));
            winnerBestHands.add(hand);
        }
        msg.put("winnerBestHands", winnerBestHands);

        broadcast(msg);

        finishGame();
    }

    // ---------- GAME END ----------
    private void finishGame() throws Exception {

        state = GameState.WAITING;
        pot = 0;
        currentBet = 0;
        minRaiseAmount = bigBlind;

        community.clear();

        currentTurn = -1;

        moveBrokePlayersToQueue();

        table.moveSpectatorsToQueue();

        table.fillSeatsFromQueue();

        if (onGameEnd != null) {
            endPauseActive = true;
            endGameScheduler.schedule(() -> {
                try {
                    endPauseActive = false;
                    onGameEnd.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, endGamePauseSeconds, TimeUnit.SECONDS);
        }
    }

    private void moveBrokePlayersToQueue() {
        List<Player> players = table.getPlayers();
        Queue<Player> queue = table.getWaitingQueue();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p == null) continue;
            if (p.getChips() > 0) continue;

            players.set(i, null);
            p.resetForRound();
            p.setChips(startingChips);
            p.setSeat(-1);
            queue.add(p);
        }
    }

    // ---------- PLAYER LEAVE ----------
    public void handlePlayerLeave(int seat) throws Exception {

        if (seat < 0) return;

        Player p = table.getPlayers().get(seat);

        if (p != null) {
            p.setFolded(true);
            p.setActed(true);
        }

        if (state == GameState.WAITING || state == GameState.SHOWDOWN) return;

        if (checkWin()) {
            return;
        }

        if (isRoundComplete()) {
            nextStage();
            return;
        }

        if (seat == currentTurn) {
            nextTurn();
        }
    }

    // ---------- BROADCAST ----------
    private void broadcast(Map<String, Object> msg) throws Exception {

        msg.put("money", buildMoneySnapshot());

        String json = mapper.writeValueAsString(msg);

        for (Player p : table.getPlayers()) {

            if (p == null) continue;

            if (p.getSession().isOpen()) {
                p.getSession().sendMessage(new TextMessage(json));
            }
        }

        for (Player s : table.getSpectators()) {

            if (s.getSession().isOpen()) {
                s.getSession().sendMessage(new TextMessage(json));
            }
        }
    }

    // ---------- EMPTY TABLE ---------
    public void handleTableEmpty() {

        if (table.getPlayerCount() == 0) {

            state = GameState.WAITING;

            community.clear();

            currentTurn = -1;
        }
    }

    // ---------- STATUS ----------
    public boolean isGameRunning() {
        return state != GameState.WAITING;
    }

    // ---------- ACCESSORS ----------
    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getDealerSeat() {
        return dealerSeat;
    }

    public GameState getState() {
        return state;
    }

    public List<String> getCommunityCards() {
        return new ArrayList<>(community);
    }

    public boolean isEndPauseActive() {
        return endPauseActive;
    }

    // ---------- STATE RESET ----------
    public void reset() {
        state = GameState.WAITING;
        pot = 0;
        currentBet = 0;
        currentTurn = -1;
        minRaiseAmount = bigBlind;
        community.clear();
    }
}
