package game;

public class Pieces {
    public short currentCnt;
    public short[] positions;
    public short[] localBoard;

    public Pieces(int count) {
        currentCnt = 0;
        positions = new short[count];
        localBoard = new short[64];
    }

    public void addPiece(short square) {
        positions[currentCnt] = square;
        localBoard[square] = currentCnt++;
    }

    public void removePiece(short square) {
        currentCnt--;
        if (currentCnt == 0) {
            return;
        }

        short positionIndex = localBoard[square];
        short squareToUpdate = positions[currentCnt];

        localBoard[squareToUpdate] = positionIndex;
        positions[positionIndex] = squareToUpdate;
    }

    public void updatePosition(short startSquare, short targetSquare) {
        short positionIndex = localBoard[startSquare];
        positions[positionIndex] = targetSquare;
        localBoard[targetSquare] = positionIndex;
    }

    public void display() {
        System.out.print("[");
        for (int i=0; i<currentCnt; i++) System.out.print(positions[i] + (i < currentCnt - 1 ? ", " : "]"));
        System.out.println();
    }
}
