package game;

import util.PieceUtil;

public class Evaluator {
    private final Manager manager;

    public Evaluator(Manager manager) {
        this.manager = manager;
    }

    public int evaluate() {
        int evaluation = getPieceValues(manager.getPlayer()) - getPieceValues(manager.getOpponent());
        return evaluation;
    }

    private int getPieceValues(Player player) {
        int value = 0;
        value += player.queens.currentCnt * PieceUtil.QUEEN_VALUE;
        value += player.rooks.currentCnt * PieceUtil.QUEEN_VALUE;
        value += player.bishops.currentCnt * PieceUtil.QUEEN_VALUE;
        value += player.knights.currentCnt * PieceUtil.QUEEN_VALUE;
        value += player.pawns.currentCnt * PieceUtil.QUEEN_VALUE;
        return value;
    }
}
