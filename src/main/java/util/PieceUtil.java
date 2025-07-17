package util;

import game.v4.Move;
import game.v4.Player;

public class PieceUtil {
    public static final byte TYPE_INVALID = -1;
    public static final byte TYPE_NONE = 0;
    public static final byte TYPE_KING = 1;
    public static final byte TYPE_QUEEN = 2;
    public static final byte TYPE_PAWN = 3;
    public static final byte TYPE_ROOK = 4;
    public static final byte TYPE_BISHOP = 5;
    public static final byte TYPE_KNIGHT = 6;
    public static final byte PIECE_MASK = 7;

    public static final int QUEEN_VALUE = 900;
    public static final int PAWN_VALUE = 100;
    public static final int ROOK_VALUE = 500;
    public static final int BISHOP_VALUE = 330;
    public static final int KNIGHT_VALUE = 300;

    public static boolean isWhitePiece(char c) {
        return c <= 'Z';
    }

    public static boolean isWhitePiece(byte b) {
        return (b & Player.WHITE) != 0;
    }

    public static byte getPieceColor(byte mask) {
        return (byte)(mask & Player.COLOR_MASK);
    }

    public static byte getPieceType(byte mask) {
        return (byte)(mask & PIECE_MASK);
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
        boolean isWhite = isWhitePiece(b);
        return switch ((b & PieceUtil.PIECE_MASK)) {
            case TYPE_KING -> isWhite ? 'K' : 'k';
            case TYPE_QUEEN -> isWhite ? 'Q' : 'q';
            case TYPE_PAWN -> isWhite ? 'P' : 'p';
            case TYPE_ROOK -> isWhite ? 'R' : 'r';
            case TYPE_BISHOP -> isWhite ? 'B' : 'b';
            case TYPE_KNIGHT -> isWhite ? 'N' : 'n';
            default -> '-';
        };
    }

    public static int getPieceValue(byte mask) {
        return switch (getPieceType(mask)) {
            case TYPE_QUEEN -> QUEEN_VALUE;
            case TYPE_PAWN -> PAWN_VALUE;
            case TYPE_ROOK -> ROOK_VALUE;
            case TYPE_BISHOP -> BISHOP_VALUE;
            case TYPE_KNIGHT -> KNIGHT_VALUE;
            default -> 0;
        };
    }

    public static boolean isQueenOrRook(byte mask) {
        byte piece = getPieceType(mask);
        return piece == TYPE_QUEEN || piece == TYPE_ROOK;
    }

    public static byte getPromotionPiece(byte move) {
        return switch (move) {
            case Move.MOVE_P_QUEEN -> TYPE_QUEEN;
            case Move.MOVE_P_ROOK -> TYPE_ROOK;
            case Move.MOVE_P_BISHOP -> TYPE_BISHOP;
            case Move.MOVE_P_KNIGHT -> TYPE_KNIGHT;
            default -> TYPE_INVALID;
        };
    }
}
