package game.v4;

import util.BoardUtil;
import util.EvaluationUtil;
import util.PieceUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class Manager {
    public byte[] board;
    public Player white;
    public Player black;
    public boolean whiteToMove;
    public byte castleRights;
    public byte epSquare;
    public byte halfMoveClock;
    public int fullMoveCnt;
    public Engine engine;

    public Manager() {
        this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public Manager(String fen) {
        white = new Player("White", Player.WHITE, EvaluationUtil.WHITE_INDEX);
        black = new Player("Black", Player.BLACK, EvaluationUtil.BLACK_INDEX);
        try {
            setBoard(fen);
            engine = new Engine(this);
        } catch (Exception e) {
            System.out.println("Couldn't initialize manager...");
            System.err.println(e.getMessage());
        }
    }

    private void setBoard(String fen) throws Exception {
        board = new byte[64];
        String[] fenParts = fen.split(" ");

        if (fenParts.length > 0 && fenParts[0].contains("k") && fenParts[0].contains("K")) {
            byte rank = 7;
            byte file = 0;
            for (char c : fenParts[0].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += (byte)(c - '0');
                } else if (c == '/') {
                    rank--;
                    file = 0;
                } else {
                    Player player = PieceUtil.isWhitePiece(c) ? white : black;
                    byte square = BoardUtil.getSquare(rank, file);

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
        halfMoveClock = fenParts.length > 4 ? Byte.parseByte(fenParts[4]) : 0;
        fullMoveCnt = fenParts.length > 5 ? Byte.parseByte(fenParts[5]) : 0;
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

    public ArrayList<Move> getLegalMoves() {
        return engine.getLegalMoves();
    }

    public Move playBotMove() {
        Move move = engine.getBestMove();
        if (move.startSquare >= 0) {
            playMove(move);
        }
        return move;
    }

    public void playMove(Move move) {
        engine.makeMove(move);
        engine.previousMoves.clear();
        long posKey = engine.zobrist.hash;
        engine.repeatedPositions.put(posKey, engine.repeatedPositions.getOrDefault(posKey, 0) + 1);
    }

    public long test(int depth) {
        return engine.test(depth);
    }
}
