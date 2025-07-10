package game;

public class Move implements Comparable<Move> {
    public final static short MOVE_DEFAULT = 0;
    public final static short MOVE_CKS = 1;
    public final static short MOVE_CQS = 2;
    public final static short MOVE_EP = 3;
    public final static short MOVE_P_QUEEN = 4;
    public final static short MOVE_P_ROOK = 5;
    public final static short MOVE_P_BISHOP = 6;
    public final static short MOVE_P_KNIGHT = 7;
    public final static short MOVE_2_SQUARES = 8;

    public short startSquare;
    public short targetSquare;
    public short moveType;
    private final int moveGain;

    public Move(short startSquare, short targetSquare, short moveType, int moveGain) {
        this.startSquare = startSquare;
        this.targetSquare = targetSquare;
        this.moveType = moveType;
        this.moveGain = moveGain;
    }

    @Override
    public int compareTo(Move m) {
        return Integer.compare(m.moveGain, this.moveGain);
    }

    public static boolean isPromotionMove(short move) {
        return move == MOVE_P_BISHOP || move == MOVE_P_KNIGHT || move == MOVE_P_ROOK || move == MOVE_P_QUEEN;
    }

    @Override
    public String toString() {
        return "[" + startSquare + " -> " + targetSquare + " : " + moveType + "]";
    }
}
