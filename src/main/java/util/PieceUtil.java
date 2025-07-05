package util;

import game.Player;

public class PieceUtil {
    public static byte TYPE_NONE = 0;
    public static byte TYPE_KING = 1;
    public static byte TYPE_QUEEN = 2;
    public static byte TYPE_PAWN = 4;
    public static byte TYPE_ROOK = 8;
    public static byte TYPE_BISHOP = 16;
    public static byte TYPE_KNIGHT = 32;

    public static boolean isWhitePiece(char c) {
        return c <= 'Z';
    }

    public static boolean isWhitePiece(byte b) {
        return (b & Player.BLACK) == 0;
    }

    public static byte getPieceMask(char c) {
        byte pieceMask = isWhitePiece(c) ? Player.WHITE : Player.BLACK;
        if (c == 'k' || c == 'K') pieceMask |= TYPE_KING;
        else if (c == 'q' || c == 'Q') pieceMask |= TYPE_QUEEN;
        else if (c == 'p' || c == 'P') pieceMask |= TYPE_PAWN;
        else if (c == 'r' || c == 'R') pieceMask |= TYPE_ROOK;
        else if (c == 'b' || c == 'B') pieceMask |= TYPE_BISHOP;
        else if (c == 'n' || c == 'N') pieceMask |= TYPE_KNIGHT;
        return pieceMask;
    }

    public static char getPieceChar(byte b) {
        char c = '-';
        if ((TYPE_KING & b) != 0) c = isWhitePiece(b) ? 'K' : 'k';
        else if ((TYPE_QUEEN & b) != 0) c = isWhitePiece(b) ? 'Q' : 'q';
        else if ((TYPE_PAWN & b) != 0) c = isWhitePiece(b) ? 'P' : 'p';
        else if ((TYPE_ROOK & b) != 0) c = isWhitePiece(b) ? 'R' : 'r';
        else if ((TYPE_BISHOP & b) != 0) c = isWhitePiece(b) ? 'B' : 'b';
        else if ((TYPE_KNIGHT & b) != 0) c = isWhitePiece(b) ? 'N' : 'n';
        return c;
    }
}
