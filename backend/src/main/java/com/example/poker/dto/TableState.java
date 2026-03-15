package com.example.poker.dto;

import java.util.List;

public class TableState {

    private String type = "table_state";
    private List<String> seats;
    private List<Integer> money;
    private int yourSeat;
    private int pot;
    private int currentBet;

    public TableState(List<String> seats2, List<Integer> money, int yourSeat, int pot, int currentBet) {
        this.seats = seats2;
        this.money = money;
        this.yourSeat = yourSeat;
        this.pot = pot;
        this.currentBet = currentBet;
    }

    public String getType() {
        return type;
    }

    public List<String> getSeats() {
        return seats;
    }

    public List<Integer> getMoney() {
        return money;
    }

    public int getYourSeat() {
        return yourSeat;
    }

    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }
}
