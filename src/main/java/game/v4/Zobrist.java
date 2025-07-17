package game.v4;

import util.BoardUtil;
import util.PieceUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;

public class Zobrist {
    public long hash;
    private long[] preComputedHash;
    private final int castleHashStart;
    private final int epHashStart;
    private long playerHash;
    private final int[] startIndexes;

    public Zobrist(byte[] board, byte castleRights, byte epSquare) {
        castleHashStart = 64 * 12;
        epHashStart = castleHashStart + 15;
        setHash();

        startIndexes = new int[12];
        for (int pieceType = 1; pieceType < 12; pieceType++) {
            startIndexes[pieceType] = pieceType * 64;
        }

        for (int square = 0; square < 64; square++) {
            int pieceType = getPieceType(board[square]);
            if (pieceType != -1) {
                hash ^= preComputedHash[startIndexes[pieceType] + square];
            }
        }

        updatePlayerHash();
        if (castleRights != 0) {
            updateCastleHash(castleRights);
        }
        if (epSquare != -1) {
            updateEPHash(epSquare);
        }
    }

    public void updatePlayerHash() {
        hash ^= playerHash;
    }

    public void updateMoveHash(byte piece, int square1, int square2) {
        int pieceType = getPieceType(piece);
        hash ^= preComputedHash[startIndexes[pieceType] + square1];
        if (square2 != -1) hash ^= preComputedHash[startIndexes[pieceType] + square2];
    }

    public void updateEPHash(byte epSquare) {
        hash ^= preComputedHash[epHashStart + BoardUtil.getFile(epSquare)];
    }

    public void updateCastleHash(byte castleMask) {
        hash ^= preComputedHash[castleHashStart + castleMask - 1];
    }

    private int getPieceType(byte mask) {
        byte pieceType = switch (PieceUtil.getPieceType(mask)) {
            case PieceUtil.TYPE_KING -> 0;
            case PieceUtil.TYPE_QUEEN -> 1;
            case PieceUtil.TYPE_PAWN -> 2;
            case PieceUtil.TYPE_ROOK -> 3;
            case PieceUtil.TYPE_BISHOP -> 4;
            case PieceUtil.TYPE_KNIGHT -> 5;
            default -> -1;
        };
        return pieceType != -1 ? pieceType + (PieceUtil.isWhitePiece(mask) ? 0 : 6) : -1;
    }

    private void setHash() {
        String fileName = "preGeneratedNumbers.txt";
        File file = new File(fileName);

        try {
            Path path = Paths.get(fileName);
            if (!file.exists()) {
                int size = 64 * 12 + 15 + 8;
                preComputedHash = new long[size];

                HashSet<Long> used = new HashSet<>();
                Random random = new Random(31279);
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < size; i++) {
                    long number = random.nextLong(0, Long.MAX_VALUE);
                    while (used.contains(number)) { // unlikely but just to be sure
                        number = random.nextLong(0, Long.MAX_VALUE);
                    }

                    used.add(number);
                    preComputedHash[i] = number;
                    sb.append(number).append(",");
                }

                long number = random.nextLong(0, Long.MAX_VALUE);
                while (used.contains(number)) {
                    number = random.nextLong(0, Long.MAX_VALUE);
                }
                playerHash = number;

                Files.write(path, sb.append(playerHash).toString().getBytes());
            } else {
                String preWrittenRandomNumbers = new String(Files.readAllBytes(path));
                String[] numbers = preWrittenRandomNumbers.split(",");
                preComputedHash = new long[numbers.length];
                for (int i = 0; i < numbers.length; i++) {
                    if (i < numbers.length - 1) preComputedHash[i] = Long.parseLong(numbers[i]);
                    else playerHash = preComputedHash[i] = Long.parseLong(numbers[i]);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating random number for zobrist hash : " + e.getMessage());
        }
    }

//    public boolean testHash(byte[] board, byte castleRights, byte epSquare) {
//        long tempHash = 0;
//        for (int square = 0; square < 64; square++) {
//            int pieceType = getPieceType(board[square]);
//            if (pieceType != -1) {
//                tempHash ^= preComputedHash[startIndexes[pieceType] + square];
//            }
//        }
//
//        if (playerHashUsed) {
//            tempHash ^= playerHash;
//        }
//        if (castleRights != 0) {
//            tempHash ^= preComputedHash[castleHashStart + castleRights - 1];
//        }
//        if (epSquare != -1) {
//            tempHash ^= preComputedHash[epHashStart + BoardUtil.getFile(epSquare)];
//        }
//        return tempHash == hash;
//    }
}
