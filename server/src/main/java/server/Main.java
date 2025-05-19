package server;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        int actualPort = server.run(8080);
        System.out.println("Server started on port: " + actualPort);
    }
}