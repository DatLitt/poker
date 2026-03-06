package com.example.poker.dto;

import java.util.List;

public class TableState {

    private String type = "table_state";
    private List<String> seats;
    private int yourSeat;

    public TableState(List<String> seats2, int yourSeat) {
        this.seats = seats2;
        this.yourSeat = yourSeat;
    }

    public String getType() {
        return type;
    }

    public List<String> getSeats() {
        return seats;
    }

    public int getYourSeat() {
        return yourSeat;
    }
}