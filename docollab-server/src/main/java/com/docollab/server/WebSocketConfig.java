package com.docollab.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DocumentWebSocketHandler documentHandler;

    public WebSocketConfig(DocumentWebSocketHandler documentHandler) {
        this.documentHandler = documentHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // This is the URL your JavaFX app will connect to: ws://localhost:8080/ws/editor
        registry.addHandler(documentHandler, "/ws/editor")
                .setAllowedOrigins("*"); // Allows any client to connect
    }
}