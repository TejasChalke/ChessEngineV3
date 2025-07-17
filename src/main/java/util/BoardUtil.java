package util;

import game.v4.Move;

import java.util.ArrayList;

public class BoardUtil {
    public static byte WHITE_KSC_MASK = 1;
    public static byte WHITE_QSC_MASK = 2;
    public static byte BLACK_KSC_MASK = 4;
    public static byte BLACK_QSC_MASK = 8;
    public static byte WHITE_CASTLE_RIGHTS = 3;
    public static byte BLACK_CASTLE_RIGHTS = 12;
    public static byte CASTLE_MASK = 15;

    public static final byte WHITE_KS_ROOK = 7;
    public static final byte WHITE_QS_ROOK = 0;
    public static final byte BLACK_KS_ROOK = 63;
    public static final byte BLACK_QS_ROOK = 56;

    public static long[] squareMask;
    public static ArrayList<Byte>[] KNIGHT_MOVES;
    public static ArrayList<Byte>[] KING_MOVES;

    public static byte[][] moveCnt;
    public static byte[] moveOffsets;
    public static boolean[][][] collinearPoints;

    static {
        // up, down, left, right, tr, tl, br, bl
        moveOffsets = new byte[] {8, -8, -1, 1, 9, 7, -7, -9};
        moveCnt = new byte[64][];
        KNIGHT_MOVES = new ArrayList[64];
        KING_MOVES = new ArrayList[64];
        squareMask = new long[64];

        for (byte rank = 0; rank < 8; rank++) {
            for (byte file = 0; file < 8; file++) {
                byte square = getSquare(rank, file);
                squareMask[square] = 1L << square;
                moveCnt[square] = new byte[] {
                        (byte)(7 - rank),
                        rank,
                        file,
                        (byte)(7 - file),
                        (byte)Math.min(7 - rank, 7 - file),
                        (byte)Math.min(7 - rank, file),
                        (byte)Math.min(rank, 7 - file),
                        (byte)Math.min(rank, file)
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

    private static ArrayList<Byte> getKnightMoves(byte rank, byte file) {
        ArrayList<Byte> moves = new ArrayList<>();
        final byte[][] dir = new byte[][] {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (byte[] d : dir) {
            byte r = (byte)(rank + d[0]);
            byte f = (byte)(file + d[1]);
            if (r >= 0 && r < 8 && f >= 0 && f < 8)
                moves.add(getSquare(r, f));
        }
        return moves;
    }

    public static byte getRank(byte square) {
        return (byte)(square / 8);
    }

    public static byte getFile(byte square) {
        return (byte)(square % 8);
    }

    public static byte getSquare(String notation) {
        if (notation.equals("-")) return -1;
        byte rank = (byte)(notation.charAt(1) - '1');
        byte file = (byte)(notation.charAt(0) - 'a');
        return getSquare(rank, file);
    }

    public static byte getSquare(byte rank, byte file) {
        return (byte)(rank * 8 + file);
    }

    public static String getUCINotation(Move move) {
        return getUCINotation(move.startSquare) + getUCINotation(move.targetSquare) + getUCIPieceNotation(move.moveType);
    }

    public static String getUCINotation(byte square) {
        char file = (char)('a' + (square % 8));
        char rank = (char)('1' + (square / 8));
        return "" + file + rank;
    }

    public static char getUCIPieceNotation(byte moveType) {
        return switch (moveType) {
            case Move.MOVE_P_QUEEN -> 'q';
            case Move.MOVE_P_ROOK -> 'r';
            case Move.MOVE_P_BISHOP -> 'b';
            case Move.MOVE_P_KNIGHT -> 'n';
            default -> '\0';
        };
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

    public static void displayBoard(byte[] board) {
        System.out.println("------------------------------");
        for (byte rank = 7; rank >= 0; rank--) {
            for (byte file = 0; file < 8; file++) {
                char c = PieceUtil.getPieceChar(board[getSquare(rank, file)]);
                System.out.printf("%3s ", c);
            }
            System.out.println();
        }
        System.out.println("------------------------------");
    }

    public static void displayMask(long attackMask) {
        System.out.println("------------------------------");
        System.out.println(Long.toBinaryString(attackMask) + " : " + Long.toBinaryString(attackMask).length());
        for (byte rank = 7; rank >= 0; rank--) {
            for (byte file = 0; file < 8; file++) {
                byte square = getSquare(rank, file);
                long squareMask = 1L << square;
                char c = (attackMask & squareMask) != 0 ? '#' : '.';
                System.out.printf("%3c ", c);
            }
            System.out.println();
        }
        System.out.println("------------------------------");
    }
}
