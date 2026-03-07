package com.example.poker.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

public class Player {

    private String name;

    private int seat;

    private WebSocketSession session;

    private int chips = 1000;

    private final List<String> cards = new ArrayList<>();

    private boolean folded = false;

    private boolean allIn = false;

    private boolean spectator = false;

    // betting system
    private int currentBet = 0;

    private boolean acted = false;

    public Player(String name, int seat, WebSocketSession session) {
        this.name = name;
        this.seat = seat;
        this.session = session;
    }

    // =========================
    // ROUND RESET
    // =========================

    public void resetForRound() {

        cards.clear();

        folded = false;

        allIn = false;

        acted = false;

        currentBet = 0;
    }

    // =========================
    // PLAYER STATUS
    // =========================

    public boolean isActive() {
        return !folded && !allIn && chips > 0 && !spectator;
    }

    public boolean canAct() {
        return !folded && !allIn && chips > 0;
    }

    // =========================
    // BETTING
    // =========================

    public void placeBet(int amount) {

        if (amount >= chips) {

            currentBet += chips;

            chips = 0;

            allIn = true;

        } else {

            chips -= amount;

            currentBet += amount;

        }

        acted = true;
    }

    public void fold() {
        folded = true;
        acted = true;
    }

    // =========================
    // GETTERS / SETTERS
    // =========================

    public String getName() {
        return name;
    }

    public int getSeat() {
        return seat;
    }

    public void setSeat(int seat) {
        this.seat = seat;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getSessionId() {
        return session.getId();
    }

    public List<String> getCards() {
        return cards;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public boolean hasActed() {
        return acted;
    }

    public void setActed(boolean acted) {
        this.acted = acted;
    }
}