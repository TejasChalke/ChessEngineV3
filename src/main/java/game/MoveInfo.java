package game;

import util.PieceUtil;

public class MoveInfo {
    public short piece;
    public short castleRights;
    public short epSquare;

    public MoveInfo(short epSquare, short castleRights) {
        this.epSquare = epSquare;
        piece = PieceUtil.TYPE_NONE;
        this.castleRights = castleRights;
    }
}