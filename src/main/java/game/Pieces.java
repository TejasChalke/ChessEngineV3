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
        if (square == 59) {
            System.out.println("Wrong add!");
        }
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
        if (targetSquare == 59 && currentCnt > 4) {
            System.out.println("Wrong update from : " + startSquare);
        }
        short positionIndex = localBoard[startSquare];
        positions[positionIndex] = targetSquare;
        localBoard[targetSquare] = positionIndex;
    }
}
