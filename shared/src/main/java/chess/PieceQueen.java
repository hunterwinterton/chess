package chess;

import java.util.Collection;
import java.util.ArrayList;

public class PieceQueen implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}, {1,0}, {-1,0}, {0,1}, {0,-1}};

        ChessPiece myPiece = board.getPiece(myPosition);

        for(int[] d: directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            while(true) {
                row += d[0];
                col += d[1];

                if(row < 1 || row > 8 || col < 1 || col > 8) {
                    break;
                }

                ChessPosition newPosition = new ChessPosition(row,col);
                ChessPiece piece = board.getPiece(newPosition);

                if(piece == null) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                } else {
                    if(piece.getTeamColor() != myPiece.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPosition, null));
                    }
                    break;
                }
            }
        }
        return moves;
    }
}
