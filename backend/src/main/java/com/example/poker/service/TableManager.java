package com.example.poker.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.example.poker.model.Player;

@Component
public class TableManager {

    private final Player[] seats = new Player[9];

    public synchronized int assignSeat(Player player) {

        for (int i = 0; i < seats.length; i++) {

            if (seats[i] == null) {
                seats[i] = player;
                return i;
            }

        }

        return -1;
    }

    public synchronized void removePlayer(WebSocketSession session) {

        for (int i = 0; i < seats.length; i++) {

            if (seats[i] != null &&
                seats[i].getSession().getId().equals(session.getId())) {

                seats[i] = null;
            }

        }

    }

    public synchronized String[] getSeatNames() {

        String[] names = new String[9];

        for (int i = 0; i < seats.length; i++) {

            if (seats[i] != null) {
                names[i] = seats[i].getName();
            } else {
                names[i] = null;
            }

        }

        return names;
    }

    public Player[] getSeats() {
        return seats;
    }
}
