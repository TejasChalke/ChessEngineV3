package game.v4;

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
        float playerEndGameWeight = (float)(EvaluationUtil.ALL_PIECES_VALUE - playerPiecesValues) / EvaluationUtil.ALL_PIECES_VALUE;
        float opponentEndGameWeight = (float)(EvaluationUtil.ALL_PIECES_VALUE - opponentPiecesValues) / EvaluationUtil.ALL_PIECES_VALUE;

        int evaluation = playerPiecesValues - opponentPiecesValues;
        evaluation += getPieceSquareValues(player, opponent, player.evalIndex, playerEndGameWeight)
                    - getPieceSquareValues(opponent, player, opponent.evalIndex, opponentEndGameWeight);

        if (Math.max(playerEndGameWeight, opponentEndGameWeight) >= 0.6) {
            evaluation += EvaluationUtil.KING_END_TABLE[player.evalIndex][player.kingSquare] - EvaluationUtil.KING_END_TABLE[opponent.evalIndex][opponent.kingSquare];
            evaluation += getKingActivityValue(player, opponent, playerEndGameWeight) - getKingActivityValue(opponent, player, opponentEndGameWeight);
        } else {
            evaluation += EvaluationUtil.KING_MID_TABLE[player.evalIndex][player.kingSquare] - EvaluationUtil.KING_MID_TABLE[opponent.evalIndex][opponent.kingSquare];
            evaluation += getKingSafetyScore(player) - getKingSafetyScore(opponent);
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

    private int getPieceSquareValues(Player player, Player opponent, int index, float endGameWeight) {
        int value = 0;
//        byte rank = 0, file = 0;
        boolean isEndGame = endGameWeight > 0.6;
//        if (isEndGame) {
//            rank = BoardUtil.getRank(player.kingSquare);
//            file = BoardUtil.getFile(player.kingSquare);
//        }

        for (int i = 0; i < player.rooks.currentCnt; i++) {
            value += EvaluationUtil.ROOK_TABLE[index][player.rooks.positions[i]];
        }
        for (int i = 0; i < player.bishops.currentCnt; i++) {
            value += EvaluationUtil.BISHOP_TABLE[index][player.bishops.positions[i]];
        }
        for (int i = 0; i < player.knights.currentCnt; i++) {
            value += EvaluationUtil.KNIGHT_TABLE[index][player.knights.positions[i]];
        }
        for (int i = 0; i < player.pawns.currentCnt; i++) {
            byte pos = player.pawns.positions[i];

            if (isEndGame) {
                value += EvaluationUtil.PAWN_END_TABLE[index][pos] * 2;
                byte rank = BoardUtil.getRank(pos);
                byte file = BoardUtil.getFile(pos);

                long fileMask = EvaluationUtil.fileMask[file] | (file > 0 ? EvaluationUtil.fileMask[file - 1] : 0) | (file < 7 ? EvaluationUtil.fileMask[file + 1] : 0);
                long completeMask = fileMask << (8 * rank);
                if ((completeMask & opponent.pawns.bitBoard) == 0) {
                    value += EvaluationUtil.passedPawnGain[rank];
                }
            } else {
                value += EvaluationUtil.PAWN_TABLE[index][pos];
            }
        }

        for (int i = 0; i < player.queens.currentCnt; i++) {
            value += EvaluationUtil.QUEEN_TABLE[index][player.queens.positions[i]];
        }
        return value;
    }

    private int getKingActivityValue(Player player, Player opponent, float endGameWeight) {
        byte rank = BoardUtil.getRank(player.kingSquare);
        byte oRank = BoardUtil.getRank(opponent.kingSquare);
        byte file = BoardUtil.getFile(player.kingSquare);
        byte oFile = BoardUtil.getFile(opponent.kingSquare);

        int cornerRankDistance = Math.min(7 - oRank, oRank);
        int cornerFileDistance = Math.min(7 - oFile, oFile);
        int opponentCornerDistance = Math.max(cornerRankDistance, cornerFileDistance);

        int kingDistance = Math.max(Math.abs(rank - oRank), Math.abs(file - oFile));
        int gain = (int)((player.color == Player.WHITE ? rank + oRank : 14 - rank - oRank) * endGameWeight * 2);
        gain += (int)((14 - kingDistance - opponentCornerDistance) * endGameWeight * 3);
        return gain;
    }

    private int getKingSafetyScore(Player player) {
        short rank = BoardUtil.getRank(player.kingSquare);
        short file = BoardUtil.getFile(player.kingSquare);

        int score = 0;
        boolean canCheck = (player.color == Player.WHITE && rank < 7) || (player.color == Player.BLACK && rank > 0);

        if (canCheck) {
            int straightOffset = player.color == Player.WHITE ? 8 : -8;
            int leftOffset = player.color == Player.WHITE ? 7 : -9;
            int rightOffset = player.color == Player.WHITE ? 9 : -7;

            if (PieceUtil.getPieceColor(manager.board[player.kingSquare + straightOffset]) == player.color) {
                score += 40;
            }
            if (file > 0 && PieceUtil.getPieceColor(manager.board[player.kingSquare + leftOffset]) == player.color) {
                score += 40;
            }
            if (file < 7 && PieceUtil.getPieceColor(manager.board[player.kingSquare + rightOffset]) == player.color) {
                score += 40;
            }
        }
        if (Player.WHITE == player.color && (player.kingSquare == 0 || player.kingSquare == 1 || player.kingSquare == 6 || player.kingSquare == 7)) score += 120;
        else if (Player.BLACK == player.color && (player.kingSquare == 56 || player.kingSquare == 57 || player.kingSquare == 62 || player.kingSquare == 63)) score += 120;
        return score;
    }
}
