package ui;

import chess.ChessPiece;
import client.WebSocketCommunicator;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ServerMessageObserver;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ErrorMessage;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.Collection;
import java.util.Scanner;

public class GameplayRepl implements ServerMessageObserver {
    private final int gameID;
    private final String role;
    private final String authToken;
    private final WebSocketCommunicator comm;
    private ChessGame currentGame;
    private boolean gameOver = false;

    public GameplayRepl(int gameID, String role, String authToken, WebSocketCommunicator comm) {
        this.gameID = gameID;
        this.role = role;
        this.authToken = authToken;
        this.comm = comm;
        comm.setObserver(this);
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] t = line.split("\\s+");
            String cmd = t[0].toUpperCase();

            switch (cmd) {
                case "HELP" -> System.out.println("HELP | MOVE | RESIGN | LEAVE | HIGHLIGHT | REDRAW");
                case "MOVE", "MAKE_MOVE" -> {
                    if (gameOver) {
                        System.out.println("Game is over. Cannot make moves.");
                        break;
                    }
                    if (t.length != 3) {
                        System.out.println("Usage: MOVE <from> <to> (MOVE e2 e4)");
                        break;
                    }
                    if (currentGame == null) {
                        System.out.println("Game not loaded yet.");
                        break;
                    }
                    ChessPosition s = parse(t[1]), e = parse(t[2]);
                    if (s == null || e == null) {
                        System.out.println("Invalid square(s). Use coordinates like e2 e4.");
                        break;
                    }
                    Collection<ChessMove> legal = currentGame.validMoves(s);
                    if (legal != null) {
                        boolean found = false;
                        for (ChessMove cm : legal) {
                            if (cm.getStartPosition().equals(s) && cm.getEndPosition().equals(e)) {
                                ChessMove mv = cm;
                                if (cm.getPromotionPiece() != null) {
                                    System.out.print("Promote pawn to (q)ueen, (r)ook, (b)ishop, (n)ight? ");
                                    String choice = sc.nextLine().trim().toLowerCase();
                                    ChessPiece.PieceType promo = ChessPiece.PieceType.QUEEN;
                                    switch (choice) {
                                        case "r": promo = ChessPiece.PieceType.ROOK; break;
                                        case "b": promo = ChessPiece.PieceType.BISHOP; break;
                                        case "n": promo = ChessPiece.PieceType.KNIGHT; break;
                                    }
                                    mv = new ChessMove(s, e, promo);
                                }
                                comm.send(new UserGameCommand(
                                        CommandType.MAKE_MOVE,
                                        authToken,
                                        gameID,
                                        mv
                                ));
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            System.out.println("Illegal move");
                        }
                    } else {
                        System.out.println("Illegal move");
                    }
                }
                case "RESIGN" -> {
                    if (!gameOver) {
                        System.out.print("Are you sure you want to resign? (y/n): ");
                        String confirmation = sc.nextLine().trim().toLowerCase();
                        if (confirmation.equals("y") || confirmation.equals("yes")) {
                            comm.send(new UserGameCommand(
                                    CommandType.RESIGN,
                                    authToken,
                                    gameID
                            ));
                            return;
                        } else {
                            System.out.println("Resign cancelled.");
                        }
                    } else {
                        System.out.println("Game is already over.");
                    }
                }
                case "LEAVE" -> {
                    comm.send(new UserGameCommand(
                            CommandType.LEAVE,
                            authToken,
                            gameID
                    ));
                    comm.close();
                    return;
                }
                case "HIGHLIGHT" -> {
                    if (currentGame == null) {
                        System.out.println("Game not loaded yet.");
                        break;
                    }
                    if (t.length != 2) {
                        System.out.println("Usage: HIGHLIGHT <square> (HIGHLIGHT e2)");
                        break;
                    }
                    ChessPosition p = parse(t[1]);
                    if (p == null) {
                        System.out.println("Invalid square. Use coordinates like e2.");
                        break;
                    }
                    if (currentGame.getBoard().getPiece(p) == null) {
                        System.out.println("No piece at " + t[1] + " to highlight.");
                        break;
                    }
                    Collection<ChessMove> hl = currentGame.validMoves(p);
                    ChessBoardDrawer.drawBoard(
                            currentGame,
                            !"BLACK".equalsIgnoreCase(role),
                            p,
                            hl
                    );
                }
                case "REDRAW" -> {
                    if (currentGame != null) {
                        ChessBoardDrawer.drawBoard(currentGame, !"BLACK".equalsIgnoreCase(role));
                    } else {
                        System.out.println("Game not loaded yet.");
                    }
                }
                default -> {
                    System.out.println("Unknown command. Type HELP for available commands.");
                }
            }
        }
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
        }
    }

    @Override
    public void onError(ErrorMessage msg) {
        System.out.println(msg.getErrorMessage());
    }
}