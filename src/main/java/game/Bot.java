package game;

import java.util.ArrayList;

public class Bot {
    private Manager manager;
    private boolean isGameOver;

    public Bot() {
        resetBot();
    }

    public Move playBotMove() {
        Move move = manager.playBotMove();
        if (move.startSquare < 0) isGameOver = true;
        return move;
    }

    public void updatePosition(Move move) {
        if (move.startSquare < 0) {
            isGameOver = true;
            System.out.println(move.startSquare);
        } else {
            manager.playMove(move);
        }
    }

    public ArrayList<Move> getLegalMoves() {
        ArrayList<Move> playerMoves = manager.getLegalMoves();
        if (playerMoves.isEmpty()) {
            isGameOver = true;
        }
        return playerMoves;
    }

    public void resetBot() {
        manager = new Manager();
        isGameOver = false;
    }

    public boolean hasGameEnded() {
        return isGameOver;
    }

    public byte[] getBoard() {
        return manager.board;
    }
}
