package game;

import util.PieceUtil;

public class MoveInfo {
    public byte piece;
    public byte castleRights;
    public byte epSquare;
    public byte halfMoveClock;

    public MoveInfo(byte epSquare, byte castleRights, byte halfMoveClock) {
        this.epSquare = epSquare;
        piece = PieceUtil.TYPE_NONE;
        this.castleRights = castleRights;
        this.halfMoveClock = halfMoveClock;
    }
}