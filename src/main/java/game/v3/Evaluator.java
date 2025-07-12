package game.v3;

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
        evaluation += getPieceSquareValues(player, player.evalIndex, playerPiecesValues - opponentPiecesValues) - getPieceSquareValues(opponent, opponent.evalIndex, opponentPiecesValues - playerPiecesValues);

        if (playerPiecesValues <= EvaluationUtil.END_GAME_VALUE_CUTOFF) {
            evaluation += EvaluationUtil.KING_END_TABLE[player.evalIndex][player.kingSquare] - EvaluationUtil.KING_END_TABLE[opponent.evalIndex][opponent.kingSquare];
            evaluation += getKingActivityValue(player, opponent, playerPiecesValues) - getKingActivityValue(opponent, player, opponentPiecesValues);
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

    private int getPieceSquareValues(Player player, int index, int pieceValues) {
        int value = 0;
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
            value += EvaluationUtil.PAWN_TABLE[index][player.pawns.positions[i]];
        }

        int queenValue = 0;
        for (int i = 0; i < player.queens.currentCnt; i++) {
            queenValue += EvaluationUtil.QUEEN_TABLE[index][player.queens.positions[i]];
        }
        // queen should not move out in the early game
        queenValue = queenValue - (EvaluationUtil.END_GAME_VALUE_CUTOFF - pieceValues) / 20;

        return value + queenValue;
    }

    private int getKingActivityValue(Player player, Player opponent, int endGameWeight) {
        short rank = BoardUtil.getRank(player.kingSquare);
        short file = BoardUtil.getFile(player.kingSquare);

        int cornerRankDistance = player.color == Player.WHITE ? 7 - rank : rank;
        int cornerFileDistance = player.color == Player.WHITE ? 7 - file : file;
        int cornerDistance = Math.max(cornerRankDistance, cornerFileDistance);

        short oRank = BoardUtil.getRank(opponent.kingSquare);
        short oFile = BoardUtil.getFile(opponent.kingSquare);
        int kingDistance = Math.max(Math.abs(rank - oRank), Math.abs(file - oFile));
        return (8 - Math.max(cornerDistance, kingDistance)) * endGameWeight;
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
