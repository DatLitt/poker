package com.example.poker.game;

import com.example.poker.model.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HandEvaluator {

    private HandEvaluator() {
    }

    public static Player determineWinner(List<Player> players, List<String> community) {
        List<Player> winners = determineWinners(players, community);
        return winners.isEmpty() ? null : winners.get(0);
    }

    public static List<Player> determineWinners(List<Player> players, List<String> community) {
        List<Player> winners = new ArrayList<>();
        HandValue bestValue = null;

        for (Player p : players) {
            HandValue value = evaluate(p.getCards(), community);
            if (bestValue == null || value.compareTo(bestValue) > 0) {
                bestValue = value;
                winners.clear();
                winners.add(p);
            } else if (value.compareTo(bestValue) == 0) {
                winners.add(p);
            }
        }

        return winners;
    }

    public static int compareHands(List<String> holeA, List<String> holeB, List<String> community) {
        HandValue a = evaluate(holeA, community);
        HandValue b = evaluate(holeB, community);
        return a.compareTo(b);
    }

    public static HandValue evaluate(List<String> holeCards, List<String> community) {
        List<Card> cards = new ArrayList<>();
        cards.addAll(parseCards(holeCards));
        cards.addAll(parseCards(community));

        if (cards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards to evaluate");
        }

        HandValue best = null;
        int n = cards.size();

        for (int i = 0; i < n - 4; i++) {
            for (int j = i + 1; j < n - 3; j++) {
                for (int k = j + 1; k < n - 2; k++) {
                    for (int l = k + 1; l < n - 1; l++) {
                        for (int m = l + 1; m < n; m++) {
                            List<Card> five = List.of(
                                    cards.get(i),
                                    cards.get(j),
                                    cards.get(k),
                                    cards.get(l),
                                    cards.get(m)
                            );
                            HandValue value = evaluateFive(five);
                            if (best == null || value.compareTo(best) > 0) {
                                best = value;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    public static List<String> bestFiveCards(List<String> holeCards, List<String> community) {
        List<Card> cards = new ArrayList<>();
        cards.addAll(parseCards(holeCards));
        cards.addAll(parseCards(community));

        if (cards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards to evaluate");
        }

        HandValue best = null;
        List<Card> bestFive = null;
        int n = cards.size();

        for (int i = 0; i < n - 4; i++) {
            for (int j = i + 1; j < n - 3; j++) {
                for (int k = j + 1; k < n - 2; k++) {
                    for (int l = k + 1; l < n - 1; l++) {
                        for (int m = l + 1; m < n; m++) {
                            List<Card> five = List.of(
                                    cards.get(i),
                                    cards.get(j),
                                    cards.get(k),
                                    cards.get(l),
                                    cards.get(m)
                            );
                            HandValue value = evaluateFive(five);
                            if (best == null || value.compareTo(best) > 0) {
                                best = value;
                                bestFive = new ArrayList<>(five);
                            }
                        }
                    }
                }
            }
        }

        if (bestFive == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Card c : bestFive) {
            result.add(c.raw);
        }
        return result;
    }

    private static HandValue evaluateFive(List<Card> cards) {
        List<Integer> ranks = new ArrayList<>();
        Map<Integer, Integer> rankCounts = new HashMap<>();
        Map<Character, Integer> suitCounts = new HashMap<>();

        for (Card c : cards) {
            ranks.add(c.rank);
            rankCounts.put(c.rank, rankCounts.getOrDefault(c.rank, 0) + 1);
            suitCounts.put(c.suit, suitCounts.getOrDefault(c.suit, 0) + 1);
        }

        ranks.sort(Collections.reverseOrder());

        boolean flush = suitCounts.values().stream().anyMatch(v -> v == 5);

        List<Integer> uniqueRanks = new ArrayList<>(rankCounts.keySet());
        uniqueRanks.sort(Collections.reverseOrder());

        Integer straightHigh = straightHigh(uniqueRanks);

        if (flush && straightHigh != null) {
            return new HandValue(8, List.of(straightHigh));
        }

        List<Integer> fours = ranksByCount(rankCounts, 4);
        if (!fours.isEmpty()) {
            int four = fours.get(0);
            int kicker = highestExcluding(ranks, List.of(four));
            return new HandValue(7, List.of(four, kicker));
        }

        List<Integer> trips = ranksByCount(rankCounts, 3);
        List<Integer> pairs = ranksByCount(rankCounts, 2);
        if (!trips.isEmpty() && !pairs.isEmpty()) {
            int trip = trips.get(0);
            int pair = pairs.get(0);
            return new HandValue(6, List.of(trip, pair));
        }

        if (flush) {
            return new HandValue(5, new ArrayList<>(ranks));
        }

        if (straightHigh != null) {
            return new HandValue(4, List.of(straightHigh));
        }

        if (!trips.isEmpty()) {
            int trip = trips.get(0);
            List<Integer> kickers = filteredRanks(ranks, List.of(trip), 2);
            List<Integer> tiebreak = new ArrayList<>();
            tiebreak.add(trip);
            tiebreak.addAll(kickers);
            return new HandValue(3, tiebreak);
        }

        if (pairs.size() >= 2) {
            int highPair = pairs.get(0);
            int lowPair = pairs.get(1);
            int kicker = highestExcluding(ranks, List.of(highPair, lowPair));
            return new HandValue(2, List.of(highPair, lowPair, kicker));
        }

        if (pairs.size() == 1) {
            int pair = pairs.get(0);
            List<Integer> kickers = filteredRanks(ranks, List.of(pair), 3);
            List<Integer> tiebreak = new ArrayList<>();
            tiebreak.add(pair);
            tiebreak.addAll(kickers);
            return new HandValue(1, tiebreak);
        }

        return new HandValue(0, new ArrayList<>(ranks));
    }

    private static Integer straightHigh(List<Integer> uniqueRanksDesc) {
        if (uniqueRanksDesc.size() != 5) {
            return null;
        }

        boolean consecutive = true;
        for (int i = 0; i < 4; i++) {
            if (uniqueRanksDesc.get(i) - 1 != uniqueRanksDesc.get(i + 1)) {
                consecutive = false;
                break;
            }
        }

        if (consecutive) {
            return uniqueRanksDesc.get(0);
        }

        if (uniqueRanksDesc.equals(List.of(14, 5, 4, 3, 2))) {
            return 5;
        }

        return null;
    }

    private static List<Integer> ranksByCount(Map<Integer, Integer> counts, int count) {
        List<Integer> ranks = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() == count) {
                ranks.add(e.getKey());
            }
        }
        ranks.sort(Collections.reverseOrder());
        return ranks;
    }

    private static int highestExcluding(List<Integer> ranksDesc, List<Integer> exclude) {
        for (int r : ranksDesc) {
            if (!exclude.contains(r)) {
                return r;
            }
        }
        return 0;
    }

    private static List<Integer> filteredRanks(List<Integer> ranksDesc, List<Integer> exclude, int limit) {
        List<Integer> result = new ArrayList<>();
        for (int r : ranksDesc) {
            if (exclude.contains(r)) continue;
            result.add(r);
            if (result.size() == limit) {
                break;
            }
        }
        return result;
    }

    private static List<Card> parseCards(List<String> raw) {
        List<Card> cards = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            String rankStr = s.substring(0, s.length() - 1);
            char suit = s.charAt(s.length() - 1);
            int rank;
            switch (rankStr) {
                case "A":
                    rank = 14;
                    break;
                case "K":
                    rank = 13;
                    break;
                case "Q":
                    rank = 12;
                    break;
                case "J":
                    rank = 11;
                    break;
                default:
                    rank = Integer.parseInt(rankStr);
                    break;
            }
            cards.add(new Card(rank, suit, s));
        }
        return cards;
    }

    private static final class Card {
        private final int rank;
        private final char suit;
        private final String raw;

        private Card(int rank, char suit, String raw) {
            this.rank = rank;
            this.suit = suit;
            this.raw = raw;
        }
    }

    public static final class HandValue implements Comparable<HandValue> {
        private final int category;
        private final List<Integer> tiebreak;

        private HandValue(int category, List<Integer> tiebreak) {
            this.category = category;
            this.tiebreak = tiebreak;
        }

        @Override
        public int compareTo(HandValue other) {
            if (this.category != other.category) {
                return Integer.compare(this.category, other.category);
            }
            int len = Math.min(this.tiebreak.size(), other.tiebreak.size());
            for (int i = 0; i < len; i++) {
                int cmp = Integer.compare(this.tiebreak.get(i), other.tiebreak.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(this.tiebreak.size(), other.tiebreak.size());
        }
    }
}
