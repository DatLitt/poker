package com.example.poker.handler;

import com.example.poker.dto.TableState;
import com.example.poker.model.Player;
import com.example.poker.service.SessionManager;
import com.example.poker.service.TableManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        SessionManager.remove(session);

        tableManager.removePlayer(session);

        broadcastTableState();
    }

    private void broadcastTableState() throws Exception {

        String[] seats = tableManager.getSeatNames();

        for (Player player : tableManager.getPlayers()) {

            TableState state = new TableState(seats, player.getSeat());

            String json = mapper.writeValueAsString(state);

            player.getSession().sendMessage(new TextMessage(json));
        }
    }
}