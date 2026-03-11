package com.example.poker.game;

import com.example.poker.model.*;
import com.example.poker.service.TableManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;

import java.util.*;

public class GameEngine {

    private GameState state = GameState.WAITING;

    private Deck deck;

    private final TableManager table;

    private final ObjectMapper mapper = new ObjectMapper();

    private final List<String> community = new ArrayList<>();

    private int currentTurn = -1;

    private int pot = 0;

    private int currentBet = 0;

    private final int smallBlind = 10;

    private final int bigBlind = 20;

    public GameEngine(TableManager table) {
        this.table = table;
    }

    // ---------- GAME START ----------
    public void startGame() throws Exception {

        if (table.getActivePlayerCount() < 2) {
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

        // post blinds
        Player sb = table.getPlayers().get(0);
        if (sb != null && sb.getChips() >= smallBlind) {
            sb.setChips(sb.getChips() - smallBlind);
            sb.setCurrentBet(smallBlind);
            pot += smallBlind;
        }
        Player bb = table.getPlayers().get(1);
        if (bb != null && bb.getChips() >= bigBlind) {
            bb.setChips(bb.getChips() - bigBlind);
            bb.setCurrentBet(bigBlind);
            pot += bigBlind;
        }
        currentBet = bigBlind;

        startBettingRound();
    }

    // ---------- BETTING ROUND ----------
    private void startBettingRound() throws Exception {

        for (Player p : table.getPlayers()) {
            if (p != null && !p.isFolded()) {
                p.setActed(false);
            }
        }

        currentTurn = findNextSeat(1); // start after BB

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
        msg.put("allowedActions", List.of("fold", "call", "raise", "all_in"));
        msg.put("pot", pot);
        msg.put("currentBet", currentBet);

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
                if (amount == null || amount <= currentBet) return;
                int raiseAmount = amount - p.getCurrentBet();
                if (raiseAmount > p.getChips() || raiseAmount <= 0) return;
                pot += raiseAmount;
                p.setChips(p.getChips() - raiseAmount);
                p.setCurrentBet(amount);
                currentBet = amount;
                // reset acted for others
                for (Player other : table.getPlayers()) {
                    if (other != null && !other.isFolded() && !other.isAllIn() && other != p) {
                        other.setActed(false);
                    }
                }
                p.setActed(true);
                break;

            case "all_in":
                int allInAmount = p.getChips();
                pot += allInAmount;
                p.setChips(0);
                p.setAllIn(true);
                p.setCurrentBet(p.getCurrentBet() + allInAmount);
                if (p.getCurrentBet() > currentBet) {
                    currentBet = p.getCurrentBet();
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

        checkWin();
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

            community.add(deck.draw());
            community.add(deck.draw());
            community.add(deck.draw());

        } else if (state == GameState.FLOP) {

            state = GameState.TURN;
            currentBet = 0;

            community.add(deck.draw());

        } else if (state == GameState.TURN) {

            state = GameState.RIVER;
            currentBet = 0;

            community.add(deck.draw());

        } else {

            showdown();
            return;
        }

        broadcastCommunity();

        startBettingRound();
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
    private void checkWin() throws Exception {

        List<Player> active = new ArrayList<>();

        for (Player p : table.getPlayers()) {
            if (p != null && !p.isFolded())
                active.add(p);
        }

        if(active.size() <= 1){
            Player winner = active.size()==1 ? active.get(0) : null;

            Map<String,Object> msg = new HashMap<>();
            msg.put("type","game_win");
            msg.put("seat", winner != null ? winner.getSeat() : -1);

            broadcast(msg);

            finishGame();
        }
    }

    // ---------- SHOWDOWN ----------
    private void showdown() throws Exception {

        Player winner = null;

        for (Player p : table.getPlayers()) {
            if (p != null && !p.isFolded())
                winner = p;
        }

        Map<String, Object> msg = new HashMap<>();

        msg.put("type", "showdown");
        msg.put("winnerSeat", winner != null ? winner.getSeat() : -1);
        msg.put("community", community);

        broadcast(msg);

        finishGame();
    }

    // ---------- GAME END ----------
    private void finishGame() throws Exception {

        state = GameState.WAITING;
        pot = 0;

        community.clear();

        currentTurn = -1;

        table.fillSeatsFromQueue();

        if (table.getPlayerCount() >= 2) {
            startGame();
        }
    }

    // ---------- PLAYER LEAVE ----------
    public void handlePlayerLeave(int seat) throws Exception {

        if (seat < 0) return;

        Player p = table.getPlayers().get(seat);

        if (p == null) return;

        p.setFolded(true);

        checkWin();
    }

    // ---------- BROADCAST ----------
    private void broadcast(Map<String, Object> msg) throws Exception {

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
}