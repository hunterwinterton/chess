package ui;

import client.ServerFacade;
import model.GameData;

import java.util.List;
import java.util.Scanner;

public class PostLoginRepl {
    private final ServerFacade serverFacade;
    private final Scanner scanner = new Scanner(System.in);
    private List<GameData> currentGames;

    public PostLoginRepl(ServerFacade serverFacade) {
        this.serverFacade = serverFacade;
    }

    public void run() {
        while (true) {
            System.out.print("[LOGGED_IN] >>> ");
            String input = scanner.nextLine().trim();
            String[] tokens = input.split("\\s+");
            if (tokens.length == 0) {
                continue;
            }

            switch (tokens[0].toLowerCase()) {
                case "help" -> printHelp();
                case "logout" -> {
                    handleLogout();
                    return;
                }
                case "create" -> handleCreate(tokens);
                case "list" -> handleList();
                case "join" -> handleJoin(tokens);
                case "observe" -> handleObserve(tokens);
                default -> System.out.println("Unknown command");
            }
        }
    }

    private void printHelp() {
        System.out.println("    create <NAME>           - create a new game");
        System.out.println("    list                    - list all games");
        System.out.println("    join <#> [WHITE|BLACK]  - join a game (by list‐index)");
        System.out.println("    observe <#>             - observe a game (by list‐index)");
        System.out.println("    logout                  - log out");
        System.out.println("    quit                    - exit");
        System.out.println("    help                    - show this message");
    }

    private void handleLogout() {
        try {
            serverFacade.logout();
            serverFacade.setAuthToken(null);
            System.out.println("Logged out successfully");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleCreate(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: create <NAME>");
            return;
        }
        try {
            int gameID = serverFacade.createGame(tokens[1]);
            currentGames = serverFacade.listGames().getGames();
            int clientIndex = -1;
            for (int i = 0; i < currentGames.size(); i++) {
                if (currentGames.get(i).gameID() == gameID) {
                    clientIndex = i + 1;
                    break;
                }
            }
            System.out.println("Game created with ID: " + clientIndex);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleList() {
        try {
            currentGames = serverFacade.listGames().getGames();
            if (currentGames.isEmpty()) {
                System.out.println("No games available");
                return;
            }
            for (int i = 0; i < currentGames.size(); i++) {
                GameData g = currentGames.get(i);
                String white = (g.whiteUsername() != null ? g.whiteUsername() : "[empty]");
                String black = (g.blackUsername() != null ? g.blackUsername() : "[empty]");
                System.out.printf("%d. %s (White: %s, Black: %s)%n",
                        i + 1, g.gameName(), white, black);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleJoin(String[] tokens) {
        if (tokens.length < 2 || tokens.length > 3) {
            System.out.println("Usage: join <#> [WHITE|BLACK]");
            return;
        }
        try {
            int idx = Integer.parseInt(tokens[1]);
            if (currentGames == null || idx < 1 || idx > currentGames.size()) {
                System.out.println("Invalid game number; use 'list' first.");
                return;
            }
            String playerColor = (tokens.length == 3 ? tokens[2].toUpperCase() : null);
            int gameID = currentGames.get(idx - 1).gameID();
            serverFacade.joinGame(gameID, playerColor);
            System.out.println("Joined game " + idx + " as " + (playerColor != null ? playerColor : "observer"));
            ChessBoardDrawer.drawBoard(playerColor != null ? playerColor : "WHITE");
        } catch (NumberFormatException ex) {
            System.out.println("Error: invalid game number");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleObserve(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Usage: observe <#>");
            return;
        }
        try {
            int idx = Integer.parseInt(tokens[1]);
            if (currentGames == null || idx < 1 || idx > currentGames.size()) {
                System.out.println("Invalid game number; use 'list' first.");
                return;
            }
            int gameID = currentGames.get(idx - 1).gameID();
            serverFacade.joinGame(gameID, "WHITE");
            System.out.println("Observing game " + idx);
            ChessBoardDrawer.drawBoard("WHITE");
        } catch (NumberFormatException ex) {
            System.out.println("Error: invalid game number");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}