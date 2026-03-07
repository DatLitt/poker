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

    private final ObjectMapper mapper=new ObjectMapper();

    private final List<String> community=new ArrayList<>();

    private int currentTurn=-1;

    public GameEngine(TableManager table){
        this.table=table;
    }

    public void startGame() throws Exception{

        state=GameState.PREFLOP;

        deck=new Deck();

        community.clear();

        for(Player p:table.getPlayers()){

            if(p==null) continue;

            p.resetForRound();

            p.getCards().add(deck.draw());
            p.getCards().add(deck.draw());

            Map<String,Object> msg=new HashMap<>();

            msg.put("type","deal_cards");
            msg.put("cards",p.getCards());

            p.getSession().sendMessage(
                new TextMessage(mapper.writeValueAsString(msg))
            );
        }

        startBettingRound();
    }
    private void startBettingRound() throws Exception{

        currentTurn=findNextSeat(-1);

        sendTurn();
    }
    private int findNextSeat(int fromSeat){

        List<Player> players = table.getPlayers();

        int size = players.size();

        for(int i=1;i<=size;i++){

            int seat = (fromSeat + i) % size;

            Player p = players.get(seat);

            if(p == null) continue;

            if(p.isFolded()) continue;

            if(p.isAllIn()) continue;

            if(p.getChips() <= 0) continue;

            return seat;
        }

        return -1;
    }
    private void sendTurn() throws Exception{

        Map<String,Object> msg=new HashMap<>();

        msg.put("type","player_turn");
        msg.put("seat",currentTurn);
        msg.put("allowedActions",List.of("fold","call","raise","all_in"));

        broadcast(msg);
    }
    public void handleAction(int seat,String action,Integer amount) throws Exception{

        Player p=table.getPlayers().get(seat);

        if(action.equals("fold"))
            p.setFolded(true);

        if(action.equals("raise"))
            p.setChips(p.getChips()-amount);

        if(action.equals("all_in"))
            p.setAllIn(true);

        checkWin();

        nextTurn();
    }
    private void nextTurn() throws Exception{

        currentTurn=findNextSeat(currentTurn);

        if(currentTurn==-1){

            nextStage();
            return;
        }

        sendTurn();
    }
    private void nextStage() throws Exception{

        if(state==GameState.PREFLOP){

            state=GameState.FLOP;

            community.add(deck.draw());
            community.add(deck.draw());
            community.add(deck.draw());

        }else if(state==GameState.FLOP){

            state=GameState.TURN;

            community.add(deck.draw());

        }else if(state==GameState.TURN){

            state=GameState.RIVER;

            community.add(deck.draw());

        }else{

            showdown();
            return;
        }

        broadcastCommunity();

        startBettingRound();
    }
    private void broadcastCommunity() throws Exception{

        Map<String,Object> msg=new HashMap<>();

        msg.put("type","community_cards");
        msg.put("cards",community);

        broadcast(msg);
    }
    private void checkWin() throws Exception{

        List<Player> active=new ArrayList<>();

        for(Player p:table.getPlayers())
            if(p!=null && !p.isFolded())
                active.add(p);

        if(active.size()==1){

            Player winner=active.get(0);

            Map<String,Object> msg=new HashMap<>();

            msg.put("type","game_win");
            msg.put("seat",winner.getSeat());

            broadcast(msg);

            startGame();
        }
    }
    private void showdown() throws Exception{

        Player winner=null;

        for(Player p:table.getPlayers())
            if(p!=null && !p.isFolded())
                winner=p;

        Map<String,Object> msg=new HashMap<>();

        msg.put("type","showdown");
        msg.put("winnerSeat",winner.getSeat());

        broadcast(msg);

        startGame();
    }
    private void broadcast(Map<String,Object> msg) throws Exception{

        String json=mapper.writeValueAsString(msg);

        for(Player p:table.getPlayers()){

            if(p!=null && p.getSession().isOpen())
                p.getSession().sendMessage(new TextMessage(json));
        }
    }
}