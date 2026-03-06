package com.example.poker.handler;

import com.example.poker.dto.TableState;
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

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        SessionManager.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        JsonNode json = mapper.readTree(message.getPayload());

        String type = json.get("type").asText();

        if ("join_table".equals(type)) {

            String name = json.get("name").asText();

            Player player = tableManager.addPlayer(name, session);

            if (player == null) {
                session.sendMessage(new TextMessage("{\"type\":\"table_full\"}"));
                return;
            }

            broadcastTableState();

            if (tableManager.shouldStartCountdown() && !tableManager.isCountdownRunning()) {

                tableManager.startCountdown(() -> {

                    try {
                        broadcastCountdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });

            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        SessionManager.remove(session);

        tableManager.removePlayer(session);

        broadcastTableState();
    }

    private void broadcastTableState() throws Exception {

        List<String> seats = tableManager.getSeatNames();

        for (Player player : tableManager.getPlayers()) {

            TableState state = new TableState(seats, player.getSeat());

            String json = mapper.writeValueAsString(state);

            player.getSession().sendMessage(new TextMessage(json));
        }
    }

    private void broadcastCountdown() throws Exception {

        int seconds = tableManager.getCountdown();

        Map<String,Object> msg = new HashMap<>();
        msg.put("type","game_countdown");
        msg.put("seconds",seconds);

        String json = mapper.writeValueAsString(msg);

        for (Player p : tableManager.getPlayers()) {

            if (p != null && p.getSession().isOpen()) {
                p.getSession().sendMessage(new TextMessage(json));
            }

        }
    }
}