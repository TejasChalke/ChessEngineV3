package game;

import util.BoardUtil;
import util.EvaluationUtil;
import util.PieceUtil;

public class Evaluator {
    private final Manager manager;

    public Evaluator(Manager manager) {
        this.manager = manager;
    }

    public int evaluate() {
        Player player = manager.getPlayer();
        Player opponent = manager.getOpponent();

        int playerPiecesValues = getPieceValues(player);
        int opponentPiecesValues = getPieceValues(opponent);

        int evaluation =  playerPiecesValues - opponentPiecesValues;
        evaluation += getPieceSquareValues(player, player.evalIndex, playerPiecesValues) - getPieceSquareValues(opponent, opponent.evalIndex, opponentPiecesValues);

        if (playerPiecesValues <= EvaluationUtil.END_GAME_VALUE_CUTOFF) {
            evaluation += EvaluationUtil.KING_END_TABLE[player.evalIndex][player.kingSquare] - EvaluationUtil.KING_END_TABLE[opponent.evalIndex][opponent.kingSquare];
            evaluation += getKingActivityValue(player, opponent, playerPiecesValues) - getKingActivityValue(opponent, player, opponentPiecesValues);
        } else {
            evaluation += EvaluationUtil.KING_MID_TABLE[player.evalIndex][player.kingSquare] - EvaluationUtil.KING_MID_TABLE[opponent.evalIndex][opponent.kingSquare];
        }
        return evaluation;
    }

    private int getPieceValues(Player player) {
        int value = 0;
        value += player.queens.currentCnt * PieceUtil.QUEEN_VALUE;
        value += player.rooks.currentCnt * PieceUtil.ROOK_VALUE;
        value += player.bishops.currentCnt * PieceUtil.BISHOP_VALUE;
        value += player.knights.currentCnt * PieceUtil.KNIGHT_VALUE;
        value += player.pawns.currentCnt * PieceUtil.PAWN_VALUE;
        return value;
    }

    private int getPieceSquareValues(Player player, int index, int pieceValues) {
        long value = 0;
        for (int i = 0; i < player.rooks.currentCnt; i++) {
            value += EvaluationUtil.ROOK_TABLE[index][player.rooks.positions[i]];
        }
        for (int i = 0; i < player.bishops.currentCnt; i++) {
            value += EvaluationUtil.BISHOP_TABLE[index][player.bishops.positions[i]];
        }
        for (int i = 0; i < player.knights.currentCnt; i++) {
            value += EvaluationUtil.KNIGHT_TABLE[index][player.knights.positions[i]];
        }

        long pawnValue = 0;
        for (int i = 0; i < player.pawns.currentCnt; i++) {
            pawnValue += EvaluationUtil.PAWN_TABLE[index][player.pawns.positions[i]];
        }
        // pawn should move ahead in the early game
        pawnValue = pawnValue * pieceValues;

        long queenValue = 0;
        for (int i = 0; i < player.queens.currentCnt; i++) {
            queenValue += EvaluationUtil.QUEEN_TABLE[index][player.queens.positions[i]];
        }
        // queen should not move out in the early game
        queenValue = queenValue * (EvaluationUtil.END_GAME_VALUE_CUTOFF - pieceValues);

        return (int)((value + pawnValue + queenValue) / 100);
    }

    private int getKingActivityValue(Player player, Player opponent, int endGameWeight) {
        short rank = BoardUtil.getRank(player.kingSquare);
        short file = BoardUtil.getFile(player.kingSquare);
        int cornerRankDistance = Math.min(7 - rank, rank);
        int cornerFileDistance = Math.min(7 - file, file);

        short oRank = BoardUtil.getRank(opponent.kingSquare);
        short oFile = BoardUtil.getFile(opponent.kingSquare);
        int kingDistance = Math.min(Math.abs(rank - oRank), Math.abs(file - oFile));
        return (Math.min(cornerRankDistance, cornerFileDistance) + kingDistance * 10) * endGameWeight;
    }
}
