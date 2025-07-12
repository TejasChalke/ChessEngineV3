package util;

public class EvaluationUtil {
    public static byte WHITE_INDEX = 0;
    public static byte BLACK_INDEX = 1;
    public static final byte[][] PAWN_TABLE = new byte[2][];
    public static final byte[][] QUEEN_TABLE = new byte[2][];
    public static final byte[][] ROOK_TABLE = new byte[2][];
    public static final byte[][] KNIGHT_TABLE = new byte[2][];
    public static final byte[][] BISHOP_TABLE = new byte[2][];
    public static final byte[][] KING_MID_TABLE = new byte[2][];
    public static final byte[][] KING_END_TABLE = new byte[2][];
    public static final int ALL_PIECES_VALUE = PieceUtil.QUEEN_VALUE + PieceUtil.ROOK_VALUE * 2 + PieceUtil.BISHOP_VALUE * 2 + PieceUtil.KNIGHT_VALUE * 2 + PieceUtil.PAWN_VALUE * 8;
    public static final int END_GAME_VALUE_CUTOFF = PieceUtil.ROOK_VALUE + PieceUtil.BISHOP_VALUE + PieceUtil.KNIGHT_VALUE + PieceUtil.PAWN_VALUE * 4;

    static {
        PAWN_TABLE[WHITE_INDEX] = new byte[] {
                0,  0,  0,  0,  0,  0,  0,  0,
                50, 50, 50,50, 50, 50, 50, 50,
                40, 40, 40,40, 40, 40, 40, 40,
                0,  0,  0, 35, 35,  0,  0,  0,
                5,  5, 10, 35, 35, 10,  5,  5,
                10, 10, 20,25, 25, 20, 10, 10,
                50, 50, 50,-10,-10, 50, 50, 50,
                0,  0,  0,  0,  0,  0,  0,  0
        };
        PAWN_TABLE[BLACK_INDEX] = mirrorForBlack(PAWN_TABLE[WHITE_INDEX]);

        KNIGHT_TABLE[WHITE_INDEX] = new byte[] {
                -50,-40,-30,-30,-30,-30,-40,-50,
                -40,-20,  0,  0,  0,  0,-20,-40,
                -30,  0, 10,15, 15, 10,  0,-30,
                -30,  5, 15,20, 20, 15,  5,-30,
                -30,  0, 15,20, 20, 15,  0,-30,
                -30,  5, 10,15, 15, 10,  5,-30,
                -40,-20,  0,  5,  5,  0,-20,-40,
                -50,-40,-30,-30,-30,-30,-40,-50
        };
        KNIGHT_TABLE[BLACK_INDEX] = mirrorForBlack(KNIGHT_TABLE[WHITE_INDEX]);

        BISHOP_TABLE[WHITE_INDEX] = new byte[] {
                -20,-10,-10,-10,-10,-10,-10,-20,
                -10,  0,  0,  0,  0,  0,  0,-10,
                -10,  0,  5, 10, 10,  5,  0,-10,
                -10,  0,  5, 10, 10,  5,  0,-10,
                -10,  5,  5, 10, 10,  5,  5,-10,
                -10,  20, 20, 5,  5, 20, 20,-10,
                -10,  5,  0,  5,  5,  0,  5,-10,
                -20,-10,-10,-10,-10,-10,-10,-20
        };
        BISHOP_TABLE[BLACK_INDEX] = mirrorForBlack(BISHOP_TABLE[WHITE_INDEX]);

        ROOK_TABLE[WHITE_INDEX] = new byte[] {
                0,  0,  0,  0,  0,  0,  0,  0,
                20, 50, 50, 50, 50, 50, 50, 20,
                -5,  0,  0,  0,  0,  0,  0, -5,
                -5,  0,  0,  0,  0,  0,  0, -5,
                -5,  0,  0,  0,  0,  0,  0, -5,
                -5,  0,  0,  0,  0,  0,  0, -5,
                -5,  0,  0,  0,  0,  0,  0, -5,
                0,   0, 20, 50, 50, 20,  0,  0
        };
        ROOK_TABLE[BLACK_INDEX] = mirrorForBlack(ROOK_TABLE[WHITE_INDEX]);

        QUEEN_TABLE[WHITE_INDEX] = new byte[] {
                -20,-10,-10, -5, -5,-10,-10,-20,
                -10,  0,  0,  0,  0,  0,  0,-10,
                -10,  0,  5,  5,  5,  5,  0,-10,
                -5,   0,  5,  5,  5,  5,  0, -5,
                 0,   0,  5,  5,  5,  5,  0, -5,
                -10,  5,  5,  5,  5,  5,  0,-10,
                -10,  0,  5,  0,  0,  0,  0,-10,
                -20,-10,-10,  0,  0,-10,-10,-20
        };
        QUEEN_TABLE[BLACK_INDEX] = mirrorForBlack(QUEEN_TABLE[WHITE_INDEX]);

        KING_MID_TABLE[WHITE_INDEX] = new byte[] {
                -30,-40,-40,-50,-50,-40,-40,-30,
                -30,-40,-40,-50,-50,-40,-40,-30,
                -30,-40,-40,-50,-50,-40,-40,-30,
                -30,-40,-40,-50,-50,-40,-40,-30,
                -20,-30,-30,-40,-40,-30,-30,-20,
                -10,-20,-20,-20,-20,-20,-20,-10,
                20, 20, -20,-20,-20,-20, 20, 20,
                25, 40, 35, -20, -20, 10, 40, 25
        };
        KING_MID_TABLE[BLACK_INDEX] = mirrorForBlack(KING_MID_TABLE[WHITE_INDEX]);

        KING_END_TABLE[WHITE_INDEX] = new byte[] {
                -50,-40,-30,-20,-20,-30,-40,-50,
                -30,-20,-10,  0,  0,-10,-20,-30,
                -30,-10, 20, 30, 30, 20,-10,-30,
                -30,-10, 30, 40, 40, 30,-10,-30,
                -30,-10, 30, 40, 40, 30,-10,-30,
                -30,-10, 20, 30, 30, 20,-10,-30,
                -30,-30,  0,  0,  0,  0,-30,-30,
                -50,-30,-30,-30,-30,-30,-30,-50
        };
        KING_END_TABLE[BLACK_INDEX] = mirrorForBlack(KING_END_TABLE[WHITE_INDEX]);
    }

    public static byte[] mirrorForBlack(byte[] whiteTable) {
        byte[] blackTable = new byte[64];
        for (byte i = 0; i < 64; i++) {
            byte rank = BoardUtil.getRank(i);
            byte file = BoardUtil.getFile(i);
            byte mirroredIndex = (byte)((7 - rank) * 8 + file);
            blackTable[i] = whiteTable[mirroredIndex];
        }
        return blackTable;
    }
}
