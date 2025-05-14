package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private ChessBoard board;
    private TeamColor teamTurn;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return this.teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return Objects.equals(board, chessGame.board) && teamTurn == chessGame.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn);
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        Collection<ChessMove> moves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : moves) {
            ChessPiece original = board.getPiece(move.getEndPosition());

            board.addPiece(move.getEndPosition(), piece);
            board.addPiece(startPosition, null);

            if (!isInCheck(piece.getTeamColor())) {
                validMoves.add(move);
            }

            board.addPiece(startPosition, piece);
            board.addPiece(move.getEndPosition(), original);
        }

        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = board.getPiece(move.getStartPosition());
        if (piece == null) {
            throw new InvalidMoveException("No piece to move there");
        } else if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("It is not that players turn");
        } else if (!validMoves(move.getStartPosition()).contains(move)) {
            throw new InvalidMoveException("This move is not valid");
        } else if (move.getPromotionPiece() == null) {
            board.addPiece(move.getEndPosition(), piece);
        } else if (move.getPromotionPiece() != null) {
            board.addPiece(move.getEndPosition(), new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
        }
        board.addPiece(move.getStartPosition(), null);
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition king = null;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == teamColor && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    king = pos;
                    break;
                }
            }
            if (king != null) {
                break;
            }
        }

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() != teamColor) {
                    for (ChessMove move : piece.pieceMoves(board, position)) {
                        if (move.getEndPosition().equals(king)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                Collection<ChessMove> moves = piece.pieceMoves(board, position);
                for (ChessMove move : moves) {
                    ChessPiece targetPiece = board.getPiece(move.getEndPosition());
                    board.addPiece(move.getEndPosition(), piece);
                    board.addPiece(position, null);

                    boolean inCheck = isInCheck(teamColor);

                    board.addPiece(position, piece);
                    board.addPiece(move.getEndPosition(), targetPiece);

                    if (!inCheck) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

        /**
         * Determines if the given team is in stalemate, which here is defined as having
         * no valid moves
         *
         * @param teamColor which team to check for stalemate
         * @return True if the specified team is in stalemate, otherwise false
         */
        public boolean isInStalemate (TeamColor teamColor){
            if (isInCheck(teamColor)) {
                return false;
            }

            for (int row = 1; row <= 8; row++) {
                for (int col = 1; col <= 8; col++) {
                    ChessPosition position = new ChessPosition(row, col);
                    ChessPiece piece = board.getPiece(position);
                    if (piece != null && piece.getTeamColor() == teamColor) {
                        if (!validMoves(position).isEmpty()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        /**
         * Sets this game's chessboard with a given board
         *
         * @param board the new board to use
         */
        public void setBoard (ChessBoard board){
            this.board = board;
        }

        /**
         * Gets the current chessboard
         *
         * @return the chessboard
         */
        public ChessBoard getBoard () {
            return this.board;
        }
    }
