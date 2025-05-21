package chess;

import java.util.ArrayList;
import java.util.Collection;

public class PieceMoves {

    public static Collection<ChessMove> moveOne(
            ChessBoard board,
            ChessPosition myPosition,
            int[][] directions,
            ChessPiece myPiece
    ) {
        Collection<ChessMove> moves = new ArrayList<>();
        for (int[] d : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            row += d[0];
            col += d[1];

            if (row < 1 || row > 8 || col < 1 || col > 8) {
                continue;
            }

            ChessPosition newPosition = new ChessPosition(row, col);
            ChessPiece piece = board.getPiece(newPosition);

            if (piece == null || piece.getTeamColor() != myPiece.getTeamColor()) {
                moves.add(new ChessMove(myPosition, newPosition, null));
            }
        }
        return moves;
    }

    public static Collection<ChessMove> moveSideways(
            ChessBoard board,
            ChessPosition myPosition,
            int[][] directions,
            ChessPiece myPiece
    ) {
        Collection<ChessMove> moves = new ArrayList<>();

        for (int[] d : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            while (true) {
                row += d[0];
                col += d[1];

                if (row < 1 || row > 8 || col < 1 || col > 8) {
                    break;
                }

                ChessPosition newPosition = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(newPosition);

                if (piece == null) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                } else {
                    if (piece.getTeamColor() != myPiece.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPosition, null));
                    }
                    break;
                }
            }
        }

        return moves;
    }
}