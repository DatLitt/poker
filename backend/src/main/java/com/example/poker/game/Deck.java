package com.example.poker.game;

import java.util.*;

public class Deck {

    private final List<String> cards = new ArrayList<>();

    public Deck(){

        String[] suits={"H","D","C","S"};
        String[] ranks={"2","3","4","5","6","7","8","9","10","J","Q","K","A"};

        for(String r:ranks)
            for(String s:suits)
                cards.add(r+s);

        Collections.shuffle(cards);
    }

    public String draw(){
        return cards.remove(0);
    }
}