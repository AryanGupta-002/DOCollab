package com.docollab.server;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    // A thread-safe list of every user currently connected to the server
    private final Set<WebSocketSession> activeSessions = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        activeSessions.add(session);
        System.out.println("New Collaborator Joined! Active users: " + activeSessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSessions.remove(session);
        System.out.println("Collaborator Disconnected. Active users: " + activeSessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        // We received a Delta (a keystroke) from a user.
        String payload = message.getPayload();

        // Broadcast this keystroke to EVERYONE EXCEPT the person who sent it
        for (WebSocketSession activeSession : activeSessions) {
            if (activeSession.isOpen() && !activeSession.getId().equals(session.getId())) {
                activeSession.sendMessage(new TextMessage(payload));
            }
        }
    }
}