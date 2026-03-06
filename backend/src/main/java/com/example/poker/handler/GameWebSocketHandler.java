package com.example.poker.handler;

import com.example.poker.model.Player;
import com.example.poker.service.TableManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final TableManager tableManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public GameWebSocketHandler(TableManager tableManager) {
        this.tableManager = tableManager;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        JsonNode json = mapper.readTree(message.getPayload());

        String type = json.get("type").asText();

        if (type.equals("join_table")) {

            String name = json.get("name").asText();

            Player player = new Player(name, -1, session);

            int seat = tableManager.assignSeat(player);

            if (seat == -1) {

                session.sendMessage(new TextMessage(
                        "{\"type\":\"table_full\"}"
                ));

                return;
            }

            player.setSeat(seat);

            sendTableState(player);

        }

    }

    private void sendTableState(Player player) throws Exception {

        String[] seats = tableManager.getSeatNames();

        var response = mapper.createObjectNode();

        response.put("type", "table_state");

        var seatArray = mapper.createArrayNode();

        for (String name : seats) {

            if (name == null)
                seatArray.addNull();
            else
                seatArray.add(name);

        }

        response.set("seats", seatArray);

        response.put("yourSeat", player.getSeat());

        player.getSession().sendMessage(
                new TextMessage(response.toString())
        );

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        tableManager.removePlayer(session);

    }

}