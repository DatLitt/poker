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

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        SessionManager.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {

            JsonNode json = mapper.readTree(message.getPayload());

            String type = json.get("type").asText();

            if ("join_table".equals(type)) {

                String name = json.get("name").asText();

                Player player = tableManager.addPlayer(name, session);

                if (player.getSeat() == -1) {
                    broadcastQueuePositions();
                    return;
                }

                broadcastTableState();

                if (player.getSeat() >= 0 && tableManager.shouldStartCountdown()) {

                    if (tableManager.isCountdownRunning()) {
                        tableManager.resetCountdown();
                    } else {

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
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        SessionManager.remove(session);

        if (tableManager.removePlayer(session) != null) { //if someone join from queue, reset countdown
            if (tableManager.isCountdownRunning()) {
                tableManager.resetCountdown();
            } else {
                tableManager.startCountdown(() -> {
                    try {
                        broadcastCountdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        broadcastTableState();
        broadcastQueuePositions();
    }

    private void broadcastTableState() throws Exception {

        List<String> seats = tableManager.getSeatNames();

        for (Player player : tableManager.getPlayers()) {

            if (player != null && player.getSession().isOpen()) {

                TableState state = new TableState(seats, player.getSeat());

                //TableState state = new TableState(seats, queue, player.getSeat());

                String json = mapper.writeValueAsString(state);

                player.getSession().sendMessage(new TextMessage(json));
            }
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

    private void broadcastQueuePositions() throws Exception {

        int position = 1;

        for (Player p : tableManager.getWaitingQueue()) {

            if (p.getSession().isOpen()) {

                Map<String,Object> msg = new HashMap<>();
                msg.put("type","queue_status");
                msg.put("position", position);

                String json = mapper.writeValueAsString(msg);

                p.getSession().sendMessage(new TextMessage(json));
            }

            position++;
        }
    }
}