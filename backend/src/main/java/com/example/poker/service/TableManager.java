package com.example.poker.service;

import com.example.poker.model.Player;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.*;

public class TableManager {

    private final int MAX_SEATS = 8;

    private final List<Player> seats =
            new ArrayList<>(Collections.nCopies(MAX_SEATS, null));

    private final Queue<Player> waitingQueue = new LinkedList<>();

    private final List<Player> spectators = new ArrayList<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> countdownTask;

    private int countdown = 10;

    // =========================
    // GETTERS
    // =========================

    public List<Player> getPlayers() {
        return seats;
    }

    public List<Player> getSpectators() {
        return spectators;
    }

    public Queue<Player> getWaitingQueue() {
        return waitingQueue;
    }

    public int getCountdown() {
        return countdown;
    }

    // =========================
    // PLAYER COUNT
    // =========================

    public int getPlayerCount() {

        int count = 0;

        for (Player p : seats) {
            if (p != null) count++;
        }

        return count;
    }

    public int getActivePlayerCount() {

        int count = 0;

        for (Player p : seats) {

            if (p == null) continue;

            if (!p.isFolded()) {
                count++;
            }
        }

        return count;
    }

    // =========================
    // ADD PLAYER
    // =========================

    public Player addPlayer(String name, WebSocketSession session) {

        for (int i = 0; i < MAX_SEATS; i++) {

            if (seats.get(i) == null) {

                Player player = new Player(name, i, session);

                seats.set(i, player);

                return player;
            }
        }

        // table full → queue
        Player queued = new Player(name, -1, session);

        waitingQueue.add(queued);

        return queued;
    }

    // =========================
    // ADD SPECTATOR
    // =========================

    public Player addSpectator(String name, WebSocketSession session) {

        Player spectator = new Player(name, -1, session);

        spectator.setSpectator(true);

        spectators.add(spectator);

        return spectator;
    }

    // =========================
    // REMOVE PLAYER
    // =========================

    public Player removePlayer(WebSocketSession session) {

        // remove from queue
        waitingQueue.removeIf(
                p -> p.getSession().getId().equals(session.getId())
        );

        // remove spectator
        spectators.removeIf(
                p -> p.getSession().getId().equals(session.getId())
        );

        // remove seated player
        for (int i = 0; i < MAX_SEATS; i++) {

            Player p = seats.get(i);

            if (p != null &&
                p.getSession().getId().equals(session.getId())) {

                seats.set(i, null);

                return p;
            }
        }

        return null;
    }

    // =========================
    // FILL SEATS FROM QUEUE
    // =========================

    public void fillSeatsFromQueue() {

        for (int i = 0; i < MAX_SEATS; i++) {

            if (seats.get(i) == null) {

                Player next = waitingQueue.poll();

                if (next == null) return;

                next.setSeat(i);

                seats.set(i, next);
            }
        }
    }

    // =========================
    // QUEUE POSITION
    // =========================

    public int getQueuePosition(Player player) {

        int pos = 1;

        for (Player p : waitingQueue) {

            if (p == player) {
                return pos;
            }

            pos++;
        }

        return -1;
    }

    // =========================
    // SEAT NAMES
    // =========================

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

    // =========================
    // FIND SEAT
    // =========================

    public int findSeatBySession(WebSocketSession session) {

        for (Player p : seats) {

            if (p != null &&
                p.getSession().getId().equals(session.getId())) {

                return p.getSeat();
            }
        }

        return -1;
    }

    // =========================
    // COUNTDOWN LOGIC
    // =========================

    public boolean shouldStartCountdown() {
        return getPlayerCount() >= 2;
    }

    public boolean isCountdownRunning() {

        return countdownTask != null &&
               !countdownTask.isDone();
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

        if (countdownTask != null &&
            !countdownTask.isDone()) {

            countdownTask.cancel(true);
        }

        countdownTask = null;
    }

    public void resetCountdown() {
        countdown = 10;
    }
}