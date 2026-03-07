package com.example.poker.service;

import com.example.poker.model.Player;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.*;

public class TableManager {

    private final Queue<Player> waitingQueue = new LinkedList<>();

    private final int MAX_SEATS = 8;

    private final List<Player> seats = new ArrayList<>(Collections.nCopies(MAX_SEATS, null));

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> countdownTask;

    private int countdown = 10;

    public List<Player> getPlayers() {
        return seats;
    }

    public int getPlayerCount() {
        int count = 0;
        for (Player p : seats) {
            if (p != null) count++;
        }
        return count;
    }

    public Player addPlayer(String name, WebSocketSession session) {

        for (int i = 0; i < MAX_SEATS; i++) {
            if (seats.get(i) == null) {

                Player player = new Player(name, i, session);
                seats.set(i, player);

                return player;
            }
        }

        // table full/queue
        Player player = new Player(name, -1, session);
        waitingQueue.add(player);

        return null;
    }

    public void removePlayer(WebSocketSession session) {

        waitingQueue.removeIf(p -> 
            p.getSession().getId().equals(session.getId())
        );

        for (int i = 0; i < MAX_SEATS; i++) {

            Player p = seats.get(i);

            if (p != null && p.getSession().getId().equals(session.getId())) {

                seats.set(i, null);

                seatNextFromQueue(i); // fill empty seat
                break;
            }

        }

        if (getPlayerCount() < 2) {
            cancelCountdown();
        }
    }

    public Player seatNextFromQueue(int seatIndex) {

        Player next = waitingQueue.poll();

        if (next != null) {

            next.setSeat(seatIndex);

            seats.set(seatIndex, next);

            resetCountdown();
        }

        return next;
    }

    public List<String> getQueueNames() {

        List<String> names = new ArrayList<>();

        for (Player p : waitingQueue) {
            names.add(p.getName());
        }

        return names;
    }

    public int getQueuePosition(Player player) {

        int position = 1;

        for (Player p : waitingQueue) {

            if (p == player) {
                return position;
            }

            position++;
        }

        return -1; // player not found
    }

    public Queue<Player> getWaitingQueue() {
        return waitingQueue;
    }

    public boolean shouldStartCountdown() {
        return getPlayerCount() >= 2;
    }

    public boolean isCountdownRunning() {
        return countdownTask != null && !countdownTask.isDone();
    }

    public void startCountdown(Runnable callback) {

        countdown = 10;

        countdownTask = scheduler.scheduleAtFixedRate(() -> {

            try {

                if (getPlayerCount() < 2) {
                    cancelCountdown();
                    return;
                }

                callback.run();

                countdown--;

                if (countdown < 0) {
                    cancelCountdown();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 1, TimeUnit.SECONDS);
    }

    public void cancelCountdown() {

        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(true);
        }

        countdownTask = null;
    }

    public void resetCountdown() {
        countdown = 10;
    }

    public int getCountdown() {
        return countdown;
    }

    public List<String> getSeatNames() {

        List<String> names = new ArrayList<>();

        for (Player p : seats) {

            if (p == null) {
                names.add(null);
            } else {
                names.add(p.getName());
            }

        }

        return names;
    }

}