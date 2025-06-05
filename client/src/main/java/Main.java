import chess.*;
import ui.PreLoginRepl;
import client.ServerFacade;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece);
        System.out.println("Type 'help' for a list of commands");

        int port = 8080;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        new PreLoginRepl(new ServerFacade(port)).run();
    }
}