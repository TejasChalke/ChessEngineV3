package game;

import util.BoardUtil;
import util.PieceUtil;

public class Manager {
    public short[] board;
    public Player white;
    public Player black;
    public boolean whiteToMove;
    public short castleRights;
    public short epSquare;
    public short halfMoveClock;
    public int fullMoveCnt;
    public Engine engine;

    public Manager() {
        this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public Manager(String fen) {
        white = new Player("White", Player.WHITE);
        black = new Player("Black", Player.BLACK);
        try {
            setBoard(fen);
            engine = new Engine(this);
        } catch (Exception e) {
            System.out.println("Couldn't initialize manager...");
            System.err.println(e.getMessage());
        }
    }

    private void setBoard(String fen) throws Exception {
        board = new short[64];
        String[] fenParts = fen.split(" ");

        if (fenParts.length > 0 && fenParts[0].contains("k") && fenParts[0].contains("K")) {
            short rank = 7;
            short file = 0;
            for (char c : fenParts[0].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += (short)(c - '0');
                } else if (c == '/') {
                    rank--;
                    file = 0;
                } else {
                    Player player = PieceUtil.isWhitePiece(c) ? white : black;
                    short square = BoardUtil.getSquare(rank, file);

                    if (c == 'k' || c == 'K') player.kingSquare = square;
                    else player.getPieces(c).addPiece(square);
                    board[square] = PieceUtil.getPieceMask(c);
                    file++;
                }
            }
        } else {
            throw new Exception("Invalid FEN passed to setup the board : " + fen);
        }

        if (fenParts.length > 1) {
            whiteToMove = fenParts[1].equals("w");
        } else {
            whiteToMove = true;
        }

        castleRights = 0;
        if (fenParts.length > 2) {
            if (fenParts[2].contains("K")) castleRights |= BoardUtil.WHITE_KSC_MASK;
            if (fenParts[2].contains("Q")) castleRights |= BoardUtil.WHITE_QSC_MASK;
            if (fenParts[2].contains("k")) castleRights |= BoardUtil.BLACK_KSC_MASK;
            if (fenParts[2].contains("q")) castleRights |= BoardUtil.BLACK_QSC_MASK;
        }

        epSquare = fenParts.length > 3 ? BoardUtil.getSquare(fenParts[3]) : -1;
        halfMoveClock = fenParts.length > 4 ? Short.parseShort(fenParts[4]) : 0;
        fullMoveCnt = fenParts.length > 5 ? Short.parseShort(fenParts[5]) : 0;
    }

    public Player getPlayer() {
        return whiteToMove ? white : black;
    }

    public Player getOpponent() {
        return whiteToMove ? black : white;
    }

    public void removePlayerCastleRights() {
        if (castleRights != 0) {
            castleRights &= (whiteToMove ? BoardUtil.BLACK_CASTLE_RIGHTS : BoardUtil.WHITE_CASTLE_RIGHTS);
        }
    }

    public boolean playBotMove() {
        Move move = engine.getBestMove();
        if (move.startSquare < 0) {
            // game has ended
            if (move.startSquare == -1) System.out.println("White won");
            else if (move.startSquare == -2) System.out.println("Black won");
            else System.out.println("Draw");

            return false;
        } else {
            playMove(move);
            return true;
        }
    }

    public void playMove(Move move) {
        engine.makeMove(move);
        engine.previousMoves.clear();
    }

    public long test(int depth) {
        return engine.test(depth);
    }
}
