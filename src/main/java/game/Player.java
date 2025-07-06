package game;

import util.PieceUtil;

public class Player {
    public static short WHITE = 0b01000000;
    public static short BLACK = 0b10000000;
    public static short COLOR_MASK = 0b11000000;

    public short kingSquare;
    public Pieces queens;
    public Pieces pawns;
    public Pieces rooks;
    public Pieces bishops;
    public Pieces knights;
    private String name;
    public short color;

    public Player(String name, short color) {
        this.name = name;
        kingSquare = -1;
        queens = new Pieces(10);
        pawns = new Pieces(8);
        rooks = new Pieces(10);
        bishops = new Pieces(10);
        knights = new Pieces(10);
        this.color = color;
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

    public Pieces getPieces(int n) {
        Pieces pieces = null;
        if (PieceUtil.TYPE_PAWN == n) pieces = pawns;
        else if (PieceUtil.TYPE_QUEEN == n) pieces = queens;
        else if (PieceUtil.TYPE_ROOK == n) pieces = rooks;
        else if (PieceUtil.TYPE_BISHOP == n) pieces = bishops;
        else if (PieceUtil.TYPE_KNIGHT == n) pieces = knights;
        return pieces;
    }
}
