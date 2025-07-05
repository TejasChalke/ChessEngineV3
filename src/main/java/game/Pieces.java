package game;

public class Pieces {
    private byte currentCnt;
    public byte[] positions;
    public byte[] localBoard;

    public Pieces(int count) {
        currentCnt = 0;
        positions = new byte[count];
        localBoard = new byte[64];
    }

    public void addPiece(byte square) {
        positions[currentCnt] = square;
        localBoard[square] = currentCnt++;
    }

    public void removePiece(byte square) {
        currentCnt--;
        if (currentCnt == 0) {
            return;
        }

        byte positionIndex = localBoard[square];
        byte squareToUpdate = positions[currentCnt];

        // more than 1 remaining
        localBoard[squareToUpdate] = positionIndex;
        positions[positionIndex] = squareToUpdate;
    }
}
