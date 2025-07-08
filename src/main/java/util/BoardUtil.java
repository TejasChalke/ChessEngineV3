package util;

import java.util.ArrayList;

public class BoardUtil {
    public static short WHITE_KSC_MASK = 1;
    public static short WHITE_QSC_MASK = 2;
    public static short BLACK_KSC_MASK = 4;
    public static short BLACK_QSC_MASK = 8;
    public static short WHITE_CASTLE_RIGHTS = 3;
    public static short BLACK_CASTLE_RIGHTS = 12;
    public static short CASTLE_MASK = 15;

    public static final short WHITE_KS_ROOK = 7;
    public static final short WHITE_QS_ROOK = 0;
    public static final short BLACK_KS_ROOK = 63;
    public static final short BLACK_QS_ROOK = 56;

    public static ArrayList<Short>[] KNIGHT_MOVES;
    public static ArrayList<Short>[] KING_MOVES;

    public static short[][] moveCnt;
    public static short[] moveOffsets;
    public static boolean[][][] collinearPoints;

    static {
        // up, down, left, right, tr, tl, br, bl
        moveOffsets = new short[] {8, -8, -1, 1, 9, 7, -7, -9};
        moveCnt = new short[64][];
        KNIGHT_MOVES = new ArrayList[64];
        KING_MOVES = new ArrayList[64];

        for (short rank = 0; rank < 8; rank++) {
            for (short file = 0; file < 8; file++) {
                short square = getSquare(rank, file);
                moveCnt[square] = new short[] {
                        (short)(7 - rank),
                        rank,
                        file,
                        (short)(7 - file),
                        (short)Math.min(7 - rank, 7 - file),
                        (short)Math.min(7 - rank, file),
                        (short)Math.min(rank, 7 - file),
                        (short)Math.min(rank, file)
                };
                KNIGHT_MOVES[square] = getKnightMoves(rank, file);
                KING_MOVES[square] = new ArrayList<>();
                if (rank > 0) {
                    KING_MOVES[square].add(getSquare((byte)(rank - 1), file));
                    if (file > 0) KING_MOVES[square].add(getSquare((byte)(rank - 1), (byte)(file - 1)));
                    if (file < 7) KING_MOVES[square].add(getSquare((byte)(rank - 1), (byte)(file + 1)));
                }
                if (rank < 7) {
                    KING_MOVES[square].add(getSquare((byte)(rank + 1), file));
                    if (file > 0) KING_MOVES[square].add(getSquare((byte)(rank + 1), (byte)(file - 1)));
                    if (file < 7) KING_MOVES[square].add(getSquare((byte)(rank + 1), (byte)(file + 1)));
                }
                if (file > 0) KING_MOVES[square].add(getSquare(rank, (byte)(file - 1)));
                if (file < 7) KING_MOVES[square].add(getSquare(rank, (byte)(file + 1)));
            }
        }

        collinearPoints = new boolean[64][64][64];
        for (int k = 0; k < 64; k++) {
            for (int p = 0; p < 64; p++) {
                for (int t = 0; t < 64; t++) {
                    collinearPoints[k][p][t] = areCollinear(k, p, t);
                }
            }
        }
    }

    private static ArrayList<Short> getKnightMoves(short rank, short file) {
        ArrayList<Short> moves = new ArrayList<>();
        final short[][] dir = new short[][] {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (short[] d : dir) {
            short r = (short)(rank + d[0]);
            short f = (short)(file + d[1]);
            if (r >= 0 && r < 8 && f >= 0 && f < 8)
                moves.add(getSquare(r, f));
        }
        return moves;
    }

    public static short getSquare(String notation) {
        if (notation.equals("-")) return -1;
        short rank = (short)(notation.charAt(1) - '1');
        short file = (short)(notation.charAt(0) - 'a');
        return getSquare(rank, file);
    }

    public static short getSquare(short rank, short file) {
        return (short)(rank * 8 + file);
    }

    public static boolean isMovingOnThePinLine(int k, int p, int t) {
        return collinearPoints[k][p][t];
    }

    // Some math stuff :-|
    public static boolean areCollinear(int k, int p, int t) {
        int[] king = toCoord(k);
        int[] piece = toCoord(p);
        int[] target = toCoord(t);

        int[] kp = normalizeVector(piece[0] - king[0], piece[1] - king[1]);
        int[] kt = normalizeVector(target[0] - king[0], target[1] - king[1]);

        return kp[0] == kt[0] && kp[1] == kt[1];
    }

    private static int[] toCoord(int pos) {
        return new int[] { pos / 8, pos % 8 };
    }

    private static int[] normalizeVector(int dr, int dc) {
        if (dr == 0 && dc == 0) return new int[] { 0, 0 };

        int g = gcd(dr, dc);
        return new int[] { dr / g, dc / g };
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a == 0 ? 1 : a;
    }

    public static void displayBoard(short[] board) {
        System.out.println("------------------------------");
//        for (short rank = 7; rank >= 0; rank--) {
//            for (short file = 0; file < 8; file++) {
//                char c = PieceUtil.getPieceChar(board[getSquare(rank, file)]);
//                System.out.printf("%3s ", c == '-' ? getSquare(rank, file) + "" : '-');
//            }
//            System.out.println();
//        }
//        System.out.println("------------------------------");
        for (short rank = 7; rank >= 0; rank--) {
            for (short file = 0; file < 8; file++) {
                char c = PieceUtil.getPieceChar(board[getSquare(rank, file)]);
                System.out.printf("%3s ", c);
            }
            System.out.println();
        }
        System.out.println("------------------------------");
    }

    public static void displayAttackMask(long attackMask) {
        System.out.println("------------------------------");
        for (short rank = 7; rank >= 0; rank--) {
            for (short file = 0; file < 8; file++) {
                short square = getSquare(rank, file);
                long squareMask = 1L << square;
                char c = (attackMask & squareMask) != 0 ? '#' : '.';
                System.out.printf("%2c ", c);
            }
            System.out.println();
        }
        System.out.println("------------------------------");
    }
}
