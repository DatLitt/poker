package com.example.poker.dto;

public class TableState {

    private String type = "table_state";
    private String[] seats;
    private int yourSeat;

    public TableState(String[] seats, int yourSeat) {
        this.seats = seats;
        this.yourSeat = yourSeat;
    }

    public String getType() {
        return type;
    }

    public String[] getSeats() {
        return seats;
    }

    public int getYourSeat() {
        return yourSeat;
    }
}