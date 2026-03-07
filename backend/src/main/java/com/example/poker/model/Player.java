package com.example.poker.model;

import org.springframework.web.socket.WebSocketSession;
import java.util.*;

public class Player {

    private String name;
    private int seat;
    private WebSocketSession session;

    private int chips = 1000;

    private List<String> cards = new ArrayList<>();

    private boolean folded = false;
    private boolean allIn = false;

    public Player(String name,int seat,WebSocketSession session){
        this.name = name;
        this.seat = seat;
        this.session = session;
    }

    public void resetForRound(){
        cards.clear();
        folded=false;
        allIn=false;
    }

    public boolean isActive(){
        return !folded && chips>0;
    }

    public String getName(){return name;}
    public int getSeat(){return seat;}
    public void setSeat(int s){seat=s;}
    public WebSocketSession getSession(){return session;}
    public List<String> getCards(){return cards;}

    public int getChips(){return chips;}
    public void setChips(int c){chips=c;}

    public boolean isFolded(){return folded;}
    public void setFolded(boolean f){folded=f;}

    public boolean isAllIn(){return allIn;}
    public void setAllIn(boolean a){allIn=a;}
}
