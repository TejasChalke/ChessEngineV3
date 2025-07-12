package game.v3;

public class Move implements Comparable<Move> {
    public final static byte MOVE_DEFAULT = 0;
    public final static byte MOVE_CKS = 1;
    public final static byte MOVE_CQS = 2;
    public final static byte MOVE_EP = 3;
    public final static byte MOVE_P_QUEEN = 4;
    public final static byte MOVE_P_ROOK = 5;
    public final static byte MOVE_P_BISHOP = 6;
    public final static byte MOVE_P_KNIGHT = 7;
    public final static byte MOVE_2_SQUARES = 8;

    public byte startSquare;
    public byte targetSquare;
    public byte moveType;
    public int moveGain;

    public Move(byte startSquare, byte targetSquare, byte moveType, int moveGain) {
        this.startSquare = startSquare;
        this.targetSquare = targetSquare;
        this.moveType = moveType;
        this.moveGain = moveGain;
    }

    @Override
    public int compareTo(Move m) {
        return Integer.compare(m.moveGain, this.moveGain);
    }

    public static boolean isPromotionMove(byte move) {
        return move == MOVE_P_BISHOP || move == MOVE_P_KNIGHT || move == MOVE_P_ROOK || move == MOVE_P_QUEEN;
    }
}
