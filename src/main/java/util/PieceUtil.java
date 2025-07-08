package util;

import game.Move;
import game.Player;

public class PieceUtil {
    public static final short TYPE_INVALID = -1;
    public static final short TYPE_NONE = 0;
    public static final short TYPE_KING = 0b00000001;
    public static final short TYPE_QUEEN = 0b00000010;
    public static final short TYPE_PAWN = 0b00000100;
    public static final short TYPE_ROOK = 0b00001000;
    public static final short TYPE_BISHOP = 0b00010000;
    public static final short TYPE_KNIGHT = 0b00100000;
    public static final short PIECE_MASK = 0b00111111;

    public static final int QUEEN_VALUE = 900;
    public static final int PAWN_VALUE = 100;
    public static final int ROOK_VALUE = 500;
    public static final int BISHOP_VALUE = 330;
    public static final int KNIGHT_VALUE = 300;

    public static boolean isWhitePiece(char c) {
        return c <= 'Z';
    }

    public static boolean isWhitePiece(short b) {
        return (b & Player.WHITE) != 0;
    }

    public static short getPieceColor(short mask) {
        return (short)(mask & Player.COLOR_MASK);
    }

    public static short getPieceType(short mask) {
        return (short)(mask & PIECE_MASK);
    }

    public static short getPieceMask(char c) {
        short pieceMask = isWhitePiece(c) ? Player.WHITE : Player.BLACK;
        if (c == 'k' || c == 'K') pieceMask |= TYPE_KING;
        else if (c == 'q' || c == 'Q') pieceMask |= TYPE_QUEEN;
        else if (c == 'p' || c == 'P') pieceMask |= TYPE_PAWN;
        else if (c == 'r' || c == 'R') pieceMask |= TYPE_ROOK;
        else if (c == 'b' || c == 'B') pieceMask |= TYPE_BISHOP;
        else if (c == 'n' || c == 'N') pieceMask |= TYPE_KNIGHT;
        return pieceMask;
    }

    public static char getPieceChar(short b) {
        char c = '-';
        if ((TYPE_KING & b) != 0) c = isWhitePiece(b) ? 'K' : 'k';
        else if ((TYPE_QUEEN & b) != 0) c = isWhitePiece(b) ? 'Q' : 'q';
        else if ((TYPE_PAWN & b) != 0) c = isWhitePiece(b) ? 'P' : 'p';
        else if ((TYPE_ROOK & b) != 0) c = isWhitePiece(b) ? 'R' : 'r';
        else if ((TYPE_BISHOP & b) != 0) c = isWhitePiece(b) ? 'B' : 'b';
        else if ((TYPE_KNIGHT & b) != 0) c = isWhitePiece(b) ? 'N' : 'n';
        return c;
    }

    public static int getPieceValue(short mask) {
        return switch (getPieceType(mask)) {
            case TYPE_QUEEN -> QUEEN_VALUE;
            case TYPE_PAWN -> PAWN_VALUE;
            case TYPE_ROOK -> ROOK_VALUE;
            case TYPE_BISHOP -> BISHOP_VALUE;
            case TYPE_KNIGHT -> KNIGHT_VALUE;
            default -> 0;
        };
    }

    public static boolean isQueenOrRook(short mask) {
        short piece = getPieceType(mask);
        return piece == TYPE_QUEEN || piece == TYPE_ROOK;
    }

    public static byte getPromotionPiece(short move) {
        return switch (move) {
            case Move.MOVE_P_QUEEN -> TYPE_QUEEN;
            case Move.MOVE_P_ROOK -> TYPE_ROOK;
            case Move.MOVE_P_BISHOP -> TYPE_BISHOP;
            case Move.MOVE_P_KNIGHT -> TYPE_KNIGHT;
            default -> TYPE_INVALID;
        };
    }
}
