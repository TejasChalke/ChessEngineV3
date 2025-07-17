package game.v4;

import util.BoardUtil;

public class Pieces {
    public byte currentCnt;
    public byte[] positions;
    public byte[] localBoard;
    public long bitBoard;

    public Pieces(int count) {
        currentCnt = 0;
        positions = new byte[count];
        localBoard = new byte[64];
    }

    public void addPiece(byte square) {
        bitBoard |= BoardUtil.squareMask[square];
        positions[currentCnt] = square;
        localBoard[square] = currentCnt++;
    }

    public void removePiece(byte square) {
        bitBoard ^= BoardUtil.squareMask[square];
        currentCnt--;
        if (currentCnt == 0) {
            return;
        }

        byte positionIndex = localBoard[square];
        byte squareToUpdate = positions[currentCnt];

        localBoard[squareToUpdate] = positionIndex;
        positions[positionIndex] = squareToUpdate;
    }

    public void updatePosition(byte startSquare, byte targetSquare) {
        bitBoard = bitBoard | BoardUtil.squareMask[targetSquare] ^ BoardUtil.squareMask[startSquare];
        byte positionIndex = localBoard[startSquare];
        positions[positionIndex] = targetSquare;
        localBoard[targetSquare] = positionIndex;
    }
}
