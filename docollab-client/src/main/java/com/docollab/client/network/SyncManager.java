package com.docollab.client.network;

import com.docollab.shared.algorithm.CRDTNode;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class SyncManager {

    private WebSocket webSocket;
    private final Gson gson;
    private final MessageListener listener;

    public interface MessageListener {
        void onNodeReceived(CRDTNode node);
    }

    public SyncManager(MessageListener listener) {
        this.listener = listener;
        this.gson = new Gson();
    }

    public void connectToServer(String username, String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            System.err.println("NETWORK: No server URL provided. Operating offline.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(serverUrl), new WebSocket.Listener() {

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            System.out.println("NETWORK: Connected to " + serverUrl + " as " + username);
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            String jsonMessage = data.toString();
                            CRDTNode receivedNode = gson.fromJson(jsonMessage, CRDTNode.class);
                            listener.onNodeReceived(receivedNode);
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            System.err.println("NETWORK ERROR: Connection lost. Falling back to offline mode.");
                        }
                    }).join();
        } catch (Exception e) {
            System.err.println("NETWORK ERROR: Could not reach server. Operating strictly offline.");
        }
    }

    public void broadcastNode(CRDTNode node) {
        if (webSocket != null) {
            String jsonPayload = gson.toJson(node);
            webSocket.sendText(jsonPayload, true);
        }
    }

    // NEW: Safely disconnects if the user wants to change servers while the app is running
    public void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "User changing connection").join();
                System.out.println("NETWORK: Disconnected from current server.");
            } catch (Exception e) {
                System.err.println("NETWORK: Error closing connection.");
            }
            webSocket = null;
        }
    }
}