package com.example.poker.handler;

import com.example.poker.service.ChatRoom;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatRoom chatRoom;

    public ChatWebSocketHandler(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        boolean joined = chatRoom.addUser(session);

        if (!joined) {
            session.sendMessage(new TextMessage("Room full (max 9 users)"));
            session.close();
            return;
        }

        broadcast("User joined. Total: " + chatRoom.getUserCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String msg = message.getPayload();
        broadcast(msg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        chatRoom.removeUser(session);
        broadcast("User left. Total: " + chatRoom.getUserCount());
    }

    private void broadcast(String message) {

        for (WebSocketSession session : chatRoom.getSessions()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception ignored) {
            }
        }
    }
}