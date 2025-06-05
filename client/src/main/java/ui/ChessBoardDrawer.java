package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import static ui.EscapeSequences.*;

public class ChessBoardDrawer {

    private static final String[] COL_LABELS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    public static void drawBoard(String perspective) {
        boolean whitePerspective = "WHITE".equalsIgnoreCase(perspective);

        ChessGame game = new ChessGame();
        ChessBoard board = game.getBoard();

        System.out.print("  ");
        for (int i = 0; i < 8; i++) {
            int colIndex = whitePerspective ? i : 7 - i;
            System.out.print(" " + COL_LABELS[colIndex] + " ");
        }
        System.out.println();

        for (int row = 0; row < 8; row++) {
            int displayRank = whitePerspective ? 8 - row : row + 1;
            System.out.print(displayRank + " ");

            int boardRowIndex = whitePerspective
                    ? 7 - row
                    :     row;

            for (int col = 0; col < 8; col++) {
                int boardColIndex = whitePerspective ? col : (7 - col);

                if ((boardRowIndex + boardColIndex) % 2 != 0) {
                    System.out.print(SET_BG_COLOR_LIGHT_GREY);
                } else {
                    System.out.print(SET_BG_COLOR_DARK_GREY);
                }

                ChessPiece piece = board.getPiece(
                        new ChessPosition(boardRowIndex + 1, boardColIndex + 1));
                System.out.print(pieceToUnicode(piece));
            }
            System.out.println(RESET_BG_COLOR + " " + displayRank);
        }

        System.out.print("  ");
        for (int i = 0; i < 8; i++) {
            int colIndex = whitePerspective ? i : 7 - i;
            System.out.print(" " + COL_LABELS[colIndex] + " ");
        }
        System.out.println();
    }

    private static String pieceToUnicode(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        }
        ChessGame.TeamColor color = piece.getTeamColor();
        ChessPiece.PieceType type = piece.getPieceType();

        return switch (type) {
            case KING -> (color == ChessGame.TeamColor.WHITE ? WHITE_KING : BLACK_KING);
            case QUEEN -> (color == ChessGame.TeamColor.WHITE ? WHITE_QUEEN : BLACK_QUEEN);
            case BISHOP -> (color == ChessGame.TeamColor.WHITE ? WHITE_BISHOP : BLACK_BISHOP);
            case KNIGHT -> (color == ChessGame.TeamColor.WHITE ? WHITE_KNIGHT : BLACK_KNIGHT);
            case ROOK -> (color == ChessGame.TeamColor.WHITE ? WHITE_ROOK : BLACK_ROOK);
            case PAWN -> (color == ChessGame.TeamColor.WHITE ? WHITE_PAWN : BLACK_PAWN);
        };
    }
}