package game.v3;

import util.PieceUtil;

public class Player {
    public static byte WHITE = 8;
    public static byte BLACK = 16;
    public static byte COLOR_MASK = 24;

    public byte kingSquare;
    public Pieces queens;
    public Pieces pawns;
    public Pieces rooks;
    public Pieces bishops;
    public Pieces knights;
    private String name;
    public byte color;
    public byte evalIndex;

    public Player(String name, byte color, byte evalIndex) {
        this.name = name;
        kingSquare = -1;
        queens = new Pieces(10);
        pawns = new Pieces(8);
        rooks = new Pieces(10);
        bishops = new Pieces(10);
        knights = new Pieces(10);
        this.color = color;
        this.evalIndex = evalIndex;
    }

    public String getName() {
        return name;
    }

    public Pieces getPieces(char c) {
        Pieces pieces = null;
        if (c == 'p' || c == 'P') pieces = pawns;
        else if (c == 'q' || c == 'Q') pieces = queens;
        else if (c == 'r' || c == 'R') pieces = rooks;
        else if (c == 'b' || c == 'B') pieces = bishops;
        else if (c == 'n' || c == 'N') pieces = knights;
        return pieces;
    }

    public Pieces getPieces(byte n) {
        byte mask = (byte)(n & PieceUtil.PIECE_MASK);
        return switch (mask) {
            case PieceUtil.TYPE_QUEEN -> queens;
            case PieceUtil.TYPE_PAWN -> pawns;
            case PieceUtil.TYPE_ROOK -> rooks;
            case PieceUtil.TYPE_BISHOP -> bishops;
            case PieceUtil.TYPE_KNIGHT -> knights;
            default -> null;
        };
    }
}
