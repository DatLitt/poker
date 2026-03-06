package com.example.poker.model;

import org.springframework.web.socket.WebSocketSession;

public class Player {

    private String name;
    private int seat;
    private WebSocketSession session;

    public Player(String name, int seat, WebSocketSession session) {
        this.name = name;
        this.seat = seat;
        this.session = session;
    }

    public String getName() { return name; }
    public int getSeat() { return seat; }
    public void setSeat(int seat) { this.seat = seat; }
    public WebSocketSession getSession() { return session; }
}
