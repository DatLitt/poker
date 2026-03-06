package com.example.poker.service;

import com.example.poker.model.Player;

import java.util.ArrayList;
import java.util.List;

public class TableManager {

    private static final int MAX_SEATS = 8;

    private Player[] seats = new Player[MAX_SEATS];

    public synchronized Player addPlayer(String name, org.springframework.web.socket.WebSocketSession session) {

        for (int i = 0; i < MAX_SEATS; i++) {
            if (seats[i] == null) {

                Player player = new Player(name, i, session);
                seats[i] = player;

                return player;
            }
        }

        return null;
    }

    public synchronized void removePlayer(org.springframework.web.socket.WebSocketSession session) {

        for (int i = 0; i < MAX_SEATS; i++) {
            if (seats[i] != null && seats[i].getSession().getId().equals(session.getId())) {
                seats[i] = null;
            }
        }
    }

    public synchronized String[] getSeatNames() {

        String[] result = new String[MAX_SEATS];

        for (int i = 0; i < MAX_SEATS; i++) {
            if (seats[i] != null) {
                result[i] = seats[i].getName();
            } else {
                result[i] = null;
            }
        }

        return result;
    }

    public List<Player> getPlayers() {

        List<Player> list = new ArrayList<>();

        for (Player p : seats) {
            if (p != null) {
                list.add(p);
            }
        }

        return list;
    }
}