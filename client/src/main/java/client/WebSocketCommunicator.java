package client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

public class WebSocketCommunicator implements WebSocket.Listener {
    private WebSocket ws;
    private final Gson gson = new Gson();
    private ServerMessageObserver observer;

    public void setObserver(ServerMessageObserver o) {
        this.observer = o;
    }

    public void connect() {
        ws = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8080/ws"), this)
                .join();
    }

    public void send(UserGameCommand cmd) {
        ws.sendText(gson.toJson(cmd), true);
    }

    public void close() {
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "").join();
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean l) {
        String json = d.toString();
        String type = gson.fromJson(json, ServerMessage.class)
                .getServerMessageType()
                .name();
        switch (type) {
            case "LOAD_GAME":
                if (observer != null) {
                    observer.onLoadGame(gson.fromJson(json, LoadGameMessage.class));
                }
                break;
            case "NOTIFICATION":
                if (observer != null) {
                    observer.onNotification(gson.fromJson(json, NotificationMessage.class));
                }
                break;
            case "ERROR":
                if (observer != null) {
                    observer.onError(gson.fromJson(json, ErrorMessage.class));
                }
                break;
        }
        w.request(1);
        return null;
    }
}