package chess;

import java.util.Collection;

public class PieceBishop implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        ChessPiece myPiece = board.getPiece(myPosition);
        return PieceMoves.moveSideways(board, myPosition, directions, myPiece);
    }
}
