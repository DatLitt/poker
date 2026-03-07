package com.example.poker.handler;

import com.example.poker.dto.TableState;
import com.example.poker.game.GameEngine;
import com.example.poker.model.Player;
import com.example.poker.service.SessionManager;
import com.example.poker.service.TableManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final TableManager tableManager = new TableManager();
    private static final GameEngine gameEngine = new GameEngine(tableManager);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SessionManager.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        try {

            JsonNode json = mapper.readTree(message.getPayload());
            String type = json.get("type").asText();

            // =========================
            // JOIN TABLE
            // =========================
            if ("join_table".equals(type)) {

                String name = json.get("name").asText();

                Player player;

                // GAME RUNNING → spectator
                if (gameEngine.isGameRunning()) {

                    player = tableManager.addSpectator(name, session);

                    sendSpectatorState(player);
                    return;
                }

                // NORMAL JOIN
                player = tableManager.addPlayer(name, session);

                // TABLE FULL → queue
                if (player.getSeat() == -1) {
                    broadcastQueuePositions();
                    return;
                }

                broadcastTableState();

                startCountdownIfReady();
            }

            // =========================
            // PLAYER ACTION
            // =========================
            if ("player_action".equals(type)) {

                String action = json.get("action").asText();

                Integer amount = null;
                if (json.has("amount")) {
                    amount = json.get("amount").asInt();
                }

                int seat = tableManager.findSeatBySession(session);

                gameEngine.handleAction(seat, action, amount);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // DISCONNECT
    // =========================
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        SessionManager.remove(session);

        int seat = tableManager.findSeatBySession(session);

        Player removed = tableManager.removePlayer(session);

        // treat leave as fold
        if (seat >= 0) {
            gameEngine.handlePlayerLeave(seat);
        }

        // move queue player into seat
        tableManager.fillSeatsFromQueue();

        broadcastTableState();
        broadcastQueuePositions();

        // only start countdown if game not running
        if (!gameEngine.isGameRunning()) {
            startCountdownIfReady();
        }
    }

    // =========================
    // COUNTDOWN CONTROL
    // =========================
    private void startCountdownIfReady() {

        if (!tableManager.shouldStartCountdown()) return;

        if (tableManager.isCountdownRunning()) {

            tableManager.resetCountdown();

        } else {

            tableManager.startCountdown(() -> {
                try {

                    broadcastCountdown();

                    if (tableManager.getCountdown() == 0) {
                        gameEngine.startGame();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // =========================
    // TABLE STATE
    // =========================
    private void broadcastTableState() throws Exception {

        List<String> seats = tableManager.getSeatNames();

        for (Player player : tableManager.getPlayers()) {

            if (player != null && player.getSession().isOpen()) {

                TableState state = new TableState(seats, player.getSeat());

                String json = mapper.writeValueAsString(state);

                player.getSession().sendMessage(new TextMessage(json));
            }
        }
    }

    // =========================
    // COUNTDOWN
    // =========================
    private void broadcastCountdown() throws Exception {

        int seconds = tableManager.getCountdown();

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "game_countdown");
        msg.put("seconds", seconds);

        String json = mapper.writeValueAsString(msg);

        for (Player p : tableManager.getPlayers()) {

            if (p != null && p.getSession().isOpen()) {
                p.getSession().sendMessage(new TextMessage(json));
            }
        }
    }

    // =========================
    // QUEUE POSITIONS
    // =========================
    private void broadcastQueuePositions() throws Exception {

        int position = 1;

        for (Player p : tableManager.getWaitingQueue()) {

            if (p.getSession().isOpen()) {

                Map<String, Object> msg = new HashMap<>();
                msg.put("type", "table_full");
                msg.put("queue", position);

                String json = mapper.writeValueAsString(msg);

                p.getSession().sendMessage(new TextMessage(json));
            }

            position++;
        }
    }

    // =========================
    // SPECTATOR STATE
    // =========================
    private void sendSpectatorState(Player spectator) throws Exception {

        Map<String, Object> msg = new HashMap<>();

        msg.put("type", "spectator_mode");
        msg.put("message", "Game in progress, you are spectating.");

        spectator.getSession().sendMessage(
                new TextMessage(mapper.writeValueAsString(msg))
        );
    }
}