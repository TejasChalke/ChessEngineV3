package util;

public class BoardUtil {
    public static byte WHITE_KS_CASTLE = 1;
    public static byte WHITE_QS_CASTLE = 2;
    public static byte BLACK_KS_CASTLE = 4;
    public static byte BLACK_QS_CASTLE = 8;

    public static byte getSquare(String notation) {
        if (notation.equals("-")) return -1;
        byte rank = (byte)(notation.charAt(1) - '1');
        byte file = (byte)(notation.charAt(0) - 'a');
        return getSquare(rank, file);
    }

    public static byte getSquare(byte rank, byte file) {
        return (byte)(rank * 8 + file);
    }

    public static void displayBoard(byte[] board) {
        for (byte rank = 7; rank >= 0; rank--) {
            for (byte file = 0; file < 8; file++) {
                System.out.printf("%2c ", PieceUtil.getPieceChar(board[getSquare(rank, file)]));
            }
            System.out.println();
        }
    }
}
