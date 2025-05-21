package chess;

import java.util.Collection;

public class PieceKnight implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        int[][] directions = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2}};
        ChessPiece myPiece = board.getPiece(myPosition);
        return PieceMoves.moveOne(board, myPosition, directions, myPiece);
    }
}
