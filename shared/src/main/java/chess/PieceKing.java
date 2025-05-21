package chess;

import java.util.Collection;

public class PieceKing implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        ChessPiece myPiece = board.getPiece(myPosition);
        return PieceMoves.moveOne(board, myPosition, directions, myPiece);
    }
}
