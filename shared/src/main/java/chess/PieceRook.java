package chess;

import java.util.Collection;

public class PieceRook implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        ChessPiece myPiece = board.getPiece(myPosition);
        return PieceMoves.moveSideways(board, myPosition, directions, myPiece);
    }
}
