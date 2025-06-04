package ui;

import client.ServerFacade;
import model.AuthData;
import java.util.Scanner;

public class PreLoginRepl {
    private final ServerFacade serverFacade;
    private final Scanner scanner = new Scanner(System.in);

    public PreLoginRepl(ServerFacade serverFacade) {
        this.serverFacade = serverFacade;
    }

    public void run() {
        System.out.println("Welcome to 240 chess. Type Help to get started.");
        while (true) {
            System.out.print("[LOGGED_OUT] >>> ");
            String input = scanner.nextLine().trim();
            String[] tokens = input.split("\\s+");
            if (tokens.length == 0) continue;

            switch (tokens[0].toLowerCase()) {
                case "help"     -> printHelp();
                case "quit"     -> System.exit(0);
                case "login"    -> handleLogin(tokens);
                case "register" -> handleRegister(tokens);
                default         -> System.out.println("Unknown command");
            }
        }
    }

    private void printHelp() {
        System.out.println("    register <USERNAME> <PASSWORD> <EMAIL> - to create an account");
        System.out.println("    login <USERNAME> <PASSWORD> - to play chess");
        System.out.println("    quit - exit");
        System.out.println("    help - show this message");
    }

    private void handleLogin(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Usage: login <USERNAME> <PASSWORD>");
            return;
        }
        try {
            AuthData authData = serverFacade.login(tokens[1], tokens[2]);
            serverFacade.setAuthToken(authData.authToken());
            System.out.println("Logged in as " + authData.username());
            new PostLoginRepl(serverFacade).run();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleRegister(String[] tokens) {
        if (tokens.length != 4) {
            System.out.println("Usage: register <USERNAME> <PASSWORD> <EMAIL>");
            return;
        }
        try {
            AuthData authData = serverFacade.register(tokens[1], tokens[2], tokens[3]);
            serverFacade.setAuthToken(authData.authToken());
            System.out.println("Logged in as " + authData.username());
            new PostLoginRepl(serverFacade).run();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}