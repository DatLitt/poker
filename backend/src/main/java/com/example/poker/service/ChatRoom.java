package com.example.poker.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class ChatRoom {

    private final Set<WebSocketSession> sessions =
            Collections.synchronizedSet(new HashSet<>());

    private final int MAX_USERS = 8;

    public boolean addUser(WebSocketSession session) {
        if (sessions.size() >= MAX_USERS) {
            return false;
        }
        sessions.add(session);
        return true;
    }

    public void removeUser(WebSocketSession session) {
        sessions.remove(session);
    }

    public Set<WebSocketSession> getSessions() {
        return sessions;
    }

    public int getUserCount() {
        return sessions.size();
    }
}