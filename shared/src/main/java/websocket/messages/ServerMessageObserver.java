package websocket.messages;

public interface ServerMessageObserver {
    void onLoadGame(LoadGameMessage m);
    void onNotification(NotificationMessage m);
    void onError(ErrorMessage m);
}