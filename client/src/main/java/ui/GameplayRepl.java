package ui;

import client.WebSocketCommunicator;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ServerMessageObserver;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ErrorMessage;
import chess.*;

import java.util.Collection;
import java.util.Scanner;

public class GameplayRepl implements ServerMessageObserver {
    private final int gameID;
    private final String role;
    private final String authToken;
    private final WebSocketCommunicator comm;
    private ChessGame currentGame;
    private boolean gameOver = false;
    private boolean shouldExit = false;

    public GameplayRepl(int gameID, String role, String authToken, WebSocketCommunicator comm) {
        this.gameID = gameID;
        this.role = role;
        this.authToken = authToken;
        this.comm = comm;
        comm.setObserver(this);
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        while (!shouldExit) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] t = line.split("\\s+");
            String cmd = t[0].toUpperCase();

            switch (cmd) {
                case "HELP" -> handleHelp();
                case "MOVE", "MAKE_MOVE" -> handleMove(t, sc);
                case "RESIGN" -> handleResign(sc);
                case "LEAVE" -> handleLeave();
                case "HIGHLIGHT" -> handleHighlight(t);
                case "REDRAW" -> handleRedraw();
                default -> handleUnknown();
            }
        }
    }

    private void handleHelp() {
        System.out.println("HELP | MOVE | RESIGN | LEAVE | HIGHLIGHT | REDRAW");
    }

    private void handleMove(String[] t, Scanner sc) {
        if (gameOver) {
            System.out.println("Game is over. Cannot make moves.");
            return;
        }
        if (t.length != 3) {
            System.out.println("Usage: MOVE <from> <to> (MOVE e2 e4)");
            return;
        }
        if (currentGame == null) {
            System.out.println("Game not loaded yet.");
            return;
        }
        ChessPosition s = parse(t[1]), e = parse(t[2]);
        if (s == null || e == null) {
            System.out.println("Invalid square(s). Use coordinates like e2 e4.");
            return;
        }
        Collection<ChessMove> legal = currentGame.validMoves(s);
        ChessMove chosen = null;
        if (legal != null) {
            for (ChessMove cm : legal) {
                if (cm.getEndPosition().equals(e)) {
                    chosen = cm;
                    break;
                }
            }
        }
        if (chosen == null) {
            System.out.println("Illegal move");
            return;
        }

        if (chosen.getPromotionPiece() != null) {
            System.out.print("Promote pawn to (q)ueen, (r)ook, (b)ishop, (n)ight? ");
            String choice = sc.nextLine().trim().toLowerCase();
            ChessPiece.PieceType promo = ChessPiece.PieceType.QUEEN;
            switch (choice) {
                case "r":
                    promo = ChessPiece.PieceType.ROOK;
                    break;
                case "b":
                    promo = ChessPiece.PieceType.BISHOP;
                    break;
                case "n":
                    promo = ChessPiece.PieceType.KNIGHT;
                    break;
            }
            chosen = new ChessMove(s, e, promo);
        }
        comm.send(new UserGameCommand(
                CommandType.MAKE_MOVE,
                authToken,
                gameID,
                chosen
        ));
    }

    private void handleResign(Scanner sc) {
        if (!gameOver) {
            System.out.print("Are you sure you want to resign? (y/n): ");
            String confirmation = sc.nextLine().trim().toLowerCase();
            if (confirmation.equals("y") || confirmation.equals("yes")) {
                comm.send(new UserGameCommand(
                        CommandType.RESIGN,
                        authToken,
                        gameID
                ));
            } else {
                System.out.println("Resign cancelled.");
            }
        } else {
            System.out.println("Game is already over.");
        }
    }

    private void handleLeave() {
        comm.send(new UserGameCommand(
                CommandType.LEAVE,
                authToken,
                gameID
        ));
        comm.close();
        shouldExit = true;
    }

    private void handleHighlight(String[] t) {
        if (currentGame == null) {
            System.out.println("Game not loaded yet.");
            return;
        }
        if (t.length != 2) {
            System.out.println("Usage: HIGHLIGHT <square> (HIGHLIGHT e2)");
            return;
        }
        ChessPosition p = parse(t[1]);
        if (p == null) {
            System.out.println("Invalid square. Use coordinates like e2.");
            return;
        }
        if (currentGame.getBoard().getPiece(p) == null) {
            System.out.println("No piece at " + t[1] + " to highlight.");
            return;
        }
        Collection<ChessMove> hl = currentGame.validMoves(p);
        ChessBoardDrawer.drawBoard(
                currentGame,
                !"BLACK".equalsIgnoreCase(role),
                p,
                hl
        );
    }

    private void handleRedraw() {
        if (currentGame != null) {
            ChessBoardDrawer.drawBoard(currentGame, !"BLACK".equalsIgnoreCase(role));
        } else {
            System.out.println("Game not loaded yet.");
        }
    }

    private void handleUnknown() {
        System.out.println("Unknown command. Type HELP for available commands.");
    }

    private ChessPosition parse(String sq) {
        if (sq == null || sq.length() != 2) {
            return null;
        }
        int col = Character.toLowerCase(sq.charAt(0)) - 'a';
        int row = sq.charAt(1) - '1';
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            return null;
        }
        return new ChessPosition(row + 1, col + 1);
    }

    @Override
    public void onLoadGame(LoadGameMessage msg) {
        currentGame = msg.getGame();
        ChessBoardDrawer.drawBoard(currentGame, !"BLACK".equalsIgnoreCase(role));
    }

    @Override
    public void onNotification(NotificationMessage msg) {
        System.out.println(msg.getMessage());
        String text = msg.getMessage().toLowerCase();
        if (text.contains("checkmate") || text.contains("stalemate") || text.contains("resign")) {
            gameOver = true;
            shouldExit = true;
        }
    }

    @Override
    public void onError(ErrorMessage msg) {
        System.out.println(msg.getErrorMessage());
    }
}