import chess.*;
import ui.PreLoginRepl;
import client.ServerFacade;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece);
        System.out.println("Type 'help' for a list of commands");

        String serverUrl = "http://localhost:8080";
        new PreLoginRepl(new ServerFacade(serverUrl)).run();
    }
}