package chess;

import java.util.Collection;
import java.util.ArrayList;

public class PiecePawn implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();

        ChessPiece myPiece = board.getPiece(myPosition);

        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        int direction = myPiece.getTeamColor() == ChessGame.TeamColor.WHITE ? 1 : -1;
        int startRow = myPiece.getTeamColor() == ChessGame.TeamColor.WHITE ? 2 : 7;
        int promotionRow = myPiece.getTeamColor() == ChessGame.TeamColor.WHITE ? 8 : 1;
        int newRow = row + direction;

        ChessPosition moveOne = new ChessPosition(newRow, col);
        if (board.getPiece(moveOne) == null) {
            if (moveOne.getRow() == promotionRow) {
                moves.add(new ChessMove(myPosition, moveOne, ChessPiece.PieceType.QUEEN));
                moves.add(new ChessMove(myPosition, moveOne, ChessPiece.PieceType.ROOK));
                moves.add(new ChessMove(myPosition, moveOne, ChessPiece.PieceType.BISHOP));
                moves.add(new ChessMove(myPosition, moveOne, ChessPiece.PieceType.KNIGHT));
            } else {
                moves.add(new ChessMove(myPosition, moveOne, null));
            }

            if (row == startRow) {
                ChessPosition moveTwo = new ChessPosition(row + direction * 2, col);
                if (board.getPiece(moveTwo) == null) {
                    moves.add(new ChessMove(myPosition, moveTwo, null));
                }
            }
        }

        int[] diagonals = {1, -1};
        for (int d : diagonals) {
            int newCol = col + d;
            if (newCol >= 1 && newCol <= 8 && newRow >= 1 && newRow <= 8) {
                ChessPosition diag = new ChessPosition(newRow, newCol);
                ChessPiece piece = board.getPiece(diag);

                if (piece != null && piece.getTeamColor() != myPiece.getTeamColor()) {
                    if (newRow == promotionRow) {
                        moves.add(new ChessMove(myPosition, diag, ChessPiece.PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, diag, ChessPiece.PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, diag, ChessPiece.PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, diag, ChessPiece.PieceType.KNIGHT));
                    } else {
                        moves.add(new ChessMove(myPosition, diag, null));
                    }
                }
            }
        }
        return moves;
    }
}
