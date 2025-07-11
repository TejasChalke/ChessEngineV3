package game;

import util.BoardUtil;
import util.PieceUtil;

import java.util.ArrayList;

public class MoveGenerator {
    private final Manager manager;
    private Player player;
    private Player opponent;
    private ArrayList<Move> legalMoves;

    public boolean isChecked;
    private boolean isDoubleChecked;
    public long attackMask;
    private long pinMask;
    private long checkMask;

    public MoveGenerator(Manager manager) {
        this.manager = manager;
    }

    public ArrayList<Move> getLegalMoves(boolean capturesOnly) {
        player = manager.getPlayer();
        opponent = manager.getOpponent();

        setAttackMask();
        generateLegalMoves(capturesOnly);
        return legalMoves;
    }

    public void setAttackMask() {
        isChecked = isDoubleChecked = false;
        attackMask = pinMask = checkMask = 0;
        setSlidingAttackMask(opponent.queens, 0, 7);
        setSlidingAttackMask(opponent.rooks, 0, 3);
        setSlidingAttackMask(opponent.bishops, 4, 7);
        setKnightAttackMask(opponent.knights);
        setPawnAttackMask(opponent.pawns);

        // king attack mask
        for (byte targetSquare : BoardUtil.KING_MOVES[opponent.kingSquare]) {
            attackMask |= BoardUtil.squareMask[targetSquare];
        }
    }

    private void setSlidingAttackMask(Pieces pieces, int dirStart, int dirEnd) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            byte pieceSquare = pieces.positions[i];

            for (int dir = dirStart; dir <= dirEnd; dir++) {
                byte maxMoves = BoardUtil.moveCnt[pieceSquare][dir];
                byte currentMoves = 0;

                long currentAttackMask = 0;
                long currentPinMask = 0;
                boolean otherPieceFound = false;

                while (currentMoves++ < maxMoves) {
                    byte targetSquare = (byte)(pieceSquare + BoardUtil.moveOffsets[dir] * currentMoves);
                    byte targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
                    long squareMask = BoardUtil.squareMask[targetSquare];

                    if (!otherPieceFound) {
                        currentAttackMask |= squareMask;
                    }
                    if (targetPiece != PieceUtil.TYPE_NONE || otherPieceFound) {
                        if (targetPiece != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color) {
                            break;
                        }

                        if (targetPiece == PieceUtil.TYPE_KING) {
                            if (!otherPieceFound) {
                                if (isChecked) isDoubleChecked = true;
                                else isChecked = true;
                                checkMask |= (currentAttackMask | BoardUtil.squareMask[pieceSquare]);

                                if (currentMoves < maxMoves) {
                                    targetSquare += BoardUtil.moveOffsets[dir];
                                    currentAttackMask |= BoardUtil.squareMask[targetSquare];
                                }
                            } else {
                                pinMask |= currentPinMask;
                            }
                            break;
                        } else if (targetPiece != PieceUtil.TYPE_NONE) {
                            if (!otherPieceFound) {
                                // first piece found
                                otherPieceFound = true;
                                currentPinMask |= squareMask;
                            } else {
                                // second piece found before the king
                                break;
                            }
                        }
                    }
                }
                attackMask |= currentAttackMask;
            }
        }
    }

    private void setKnightAttackMask(Pieces pieces) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            byte pieceSquare = pieces.positions[i];

            for (byte targetSquare : BoardUtil.KNIGHT_MOVES[pieceSquare]) {
                attackMask |= BoardUtil.squareMask[targetSquare];

                if (targetSquare == player.kingSquare) {
                    if (isChecked) isDoubleChecked = true;
                    else isChecked = true;
                    checkMask |= BoardUtil.squareMask[pieceSquare];
                }
            }
        }
    }

    private void setPawnAttackMask(Pieces pieces) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            byte pieceSquare = pieces.positions[i];
            byte file = (byte)(pieceSquare % 8);

            if (file > 0) {
                int targetSquare = opponent.color == Player.WHITE ? pieceSquare + 7 : pieceSquare - 9;
                attackMask |= BoardUtil.squareMask[targetSquare];

                if (targetSquare == player.kingSquare) {
                    if (isChecked) isDoubleChecked = true;
                    else isChecked = true;
                    checkMask |= BoardUtil.squareMask[pieceSquare];
                }
            }
            if (file < 7) {
                int targetSquare = opponent.color == Player.WHITE ? pieceSquare + 9 : pieceSquare - 7;
                attackMask |= BoardUtil.squareMask[targetSquare];

                if (targetSquare == player.kingSquare) {
                    if (isChecked) isDoubleChecked = true;
                    else isChecked = true;
                    checkMask |= BoardUtil.squareMask[pieceSquare];
                }
            }
        }
    }

    private boolean isSquareSafe(byte square) {
        return (BoardUtil.squareMask[square] & attackMask) == 0;
    }

    private boolean isSquarePinned(byte square) {
        return (BoardUtil.squareMask[square] & pinMask) != 0;
    }

    private boolean isBlockingCheck(byte square) {
        return (BoardUtil.squareMask[square] & checkMask) != 0;
    }

    private void generateLegalMoves(boolean capturesOnly) {
        legalMoves = new ArrayList<>();

        // king moves
        for (byte targetSquare : BoardUtil.KING_MOVES[player.kingSquare]) {
            if (isSquareSafe(targetSquare)) {
                byte targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
                if (targetPiece == PieceUtil.TYPE_NONE && !capturesOnly) {
                    legalMoves.add(new Move(player.kingSquare, targetSquare, Move.MOVE_DEFAULT, 0));
                } else if (targetPiece != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color) {
                    legalMoves.add(new Move(player.kingSquare, targetSquare, Move.MOVE_DEFAULT, PieceUtil.getPieceValue(targetPiece)));
                }
            }
        }
        if (isDoubleChecked) return;

        boolean canCastleKingSide = (player.color == Player.WHITE ? (manager.castleRights & BoardUtil.WHITE_KSC_MASK) : (manager.castleRights & BoardUtil.BLACK_KSC_MASK)) != 0;
        boolean canCastleQueenSide = (player.color == Player.WHITE ? (manager.castleRights & BoardUtil.WHITE_QSC_MASK) : (manager.castleRights & BoardUtil.BLACK_QSC_MASK)) != 0;

        // Castling moves
        if (!isChecked && canCastleKingSide && manager.board[player.kingSquare + 1] == PieceUtil.TYPE_NONE && isSquareSafe((byte)(player.kingSquare + 1)) && manager.board[player.kingSquare + 2] == PieceUtil.TYPE_NONE && isSquareSafe((byte)(player.kingSquare + 2))) {
            legalMoves.add(new Move(player.kingSquare, (byte)(player.kingSquare + 2), Move.MOVE_CKS, 2000));
        }
        if (!isChecked && canCastleQueenSide && manager.board[player.kingSquare - 1] == PieceUtil.TYPE_NONE && isSquareSafe((byte)(player.kingSquare - 1)) && manager.board[player.kingSquare - 2] == PieceUtil.TYPE_NONE && isSquareSafe((byte)(player.kingSquare - 2)) && manager.board[player.kingSquare - 3] == PieceUtil.TYPE_NONE) {
            legalMoves.add(new Move(player.kingSquare, (byte)(player.kingSquare - 2), Move.MOVE_CQS, 2000));
        }

        generateSlidingMoves(player.queens, 0, 7, capturesOnly);
        generateSlidingMoves(player.rooks, 0, 3, capturesOnly);
        generateSlidingMoves(player.bishops, 4, 7, capturesOnly);
        generateKnightMoves(player.knights, capturesOnly);
        generatePawnMoves(player.pawns, capturesOnly);
    }

    private void generateSlidingMoves(Pieces pieces, int dirStart, int dirEnd, boolean capturesOnly) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            byte pieceSquare = pieces.positions[i];

            for (int dir = dirStart; dir <= dirEnd; dir++) {
                byte maxMoves = BoardUtil.moveCnt[pieceSquare][dir];
                if (maxMoves > 0 && isSquarePinned(pieceSquare) && !BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, pieceSquare + BoardUtil.moveOffsets[dir])) {
                    // the pinned piece must move in the direction of the pin
                    continue;
                }

                byte currentMoves = 0;
                while (currentMoves++ < maxMoves) {
                    byte targetSquare = (byte)(pieceSquare + BoardUtil.moveOffsets[dir] * currentMoves);
                    byte targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
                    byte targetPieceColor = PieceUtil.getPieceColor(manager.board[targetSquare]);

                    if (isChecked && !isBlockingCheck(targetSquare)) {
                        if (targetPiece != PieceUtil.TYPE_NONE) break;
                        else continue;
                    }

                    if (targetPiece != PieceUtil.TYPE_NONE) {
                        if (targetPieceColor == opponent.color) {
                            int gain = PieceUtil.getPieceValue(targetPiece) - PieceUtil.getPieceValue(manager.board[pieceSquare]) / 10;
                            legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, gain));
                        }
                        break;
                    } else if (!capturesOnly) {
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, 0));
                    }
                }
            }
        }
    }

    private void generateKnightMoves(Pieces pieces, boolean capturesOnly) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            byte pieceSquare = pieces.positions[i];
            if (isSquarePinned(pieceSquare)) {
                // pinned knight can not move
                continue;
            }

            for (byte targetSquare : BoardUtil.KNIGHT_MOVES[pieceSquare]) {
                if (isChecked && !isBlockingCheck(targetSquare)) {
                    continue;
                }

                byte targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
                if (targetPiece == PieceUtil.TYPE_NONE && !capturesOnly) {
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, 0));
                } else if (targetPiece != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color) {
                    int gain = PieceUtil.getPieceValue(targetPiece) - PieceUtil.KNIGHT_VALUE / 10;
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, gain));
                }
            }
        }
    }

    private void generatePawnMoves(Pieces pieces, boolean capturesOnly) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            byte pieceSquare = pieces.positions[i];
            byte rank = (byte)(pieceSquare / 8);
            byte file = (byte)(pieceSquare % 8);
            boolean isPawnPinned = isSquarePinned(pieceSquare);
            byte leftOffset, rightOffset, straightOffset, promotionRank, twoSquaresAheadRank;

            if (player.color == Player.WHITE) {
                straightOffset = 8;
                leftOffset = 7;
                rightOffset = 9;
                promotionRank = 6;
                twoSquaresAheadRank = 1;
            } else {
                straightOffset = -8;
                leftOffset = -9;
                rightOffset = -7;
                promotionRank = 1;
                twoSquaresAheadRank = 6;
            }

            // move ahead
            byte targetSquare = (byte)(pieceSquare + straightOffset);
            if (manager.board[targetSquare] == PieceUtil.TYPE_NONE && !capturesOnly && (!isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, targetSquare))) {
                if (!isChecked || isBlockingCheck(targetSquare)) {
                    if (rank == promotionRank) {
                        // promotion moves
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_QUEEN, PieceUtil.QUEEN_VALUE));
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_ROOK, PieceUtil.ROOK_VALUE));
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_BISHOP, PieceUtil.BISHOP_VALUE));
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_KNIGHT, PieceUtil.KNIGHT_VALUE));
                    } else {
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, 0));
                    }
                }

                targetSquare += straightOffset;
                if (rank == twoSquaresAheadRank && manager.board[targetSquare] == PieceUtil.TYPE_NONE && (!isChecked || isBlockingCheck(targetSquare))) {
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_2_SQUARES, 0));
                }
            }

            // capture piece left
            targetSquare = (byte)(pieceSquare + leftOffset);
            if (file > 0 && manager.board[targetSquare] != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color
                    && (!isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, targetSquare))
                    && (!isChecked || isBlockingCheck(targetSquare))) {
                int opponentPieceValue = PieceUtil.getPieceValue(manager.board[targetSquare]);
                if (rank == promotionRank) {
                    // promotion moves
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_QUEEN, PieceUtil.QUEEN_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_ROOK, PieceUtil.ROOK_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_BISHOP, PieceUtil.BISHOP_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_KNIGHT, PieceUtil.KNIGHT_VALUE + opponentPieceValue));
                } else {
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, opponentPieceValue));
                }
            }

            // capture piece right
            targetSquare = (byte)(pieceSquare + rightOffset);
            if (file < 7 && manager.board[targetSquare] != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color
                    && (!isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, targetSquare))
                    && (!isChecked || isBlockingCheck(targetSquare))) {
                int opponentPieceValue = PieceUtil.getPieceValue(manager.board[targetSquare]);
                if (rank == promotionRank) {
                    // promotion moves
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_QUEEN, PieceUtil.QUEEN_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_ROOK, PieceUtil.ROOK_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_BISHOP, PieceUtil.BISHOP_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_P_KNIGHT, PieceUtil.KNIGHT_VALUE + opponentPieceValue));
                } else {
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, opponentPieceValue));
                }
            }

            // capture en passant
            if (file > 0 && manager.epSquare == pieceSquare + leftOffset) {
                generateEPMove(rank, file, pieceSquare, leftOffset, straightOffset, isPawnPinned);
            } else if (file < 7 && manager.epSquare == pieceSquare + rightOffset) {
                generateEPMove(rank, file, pieceSquare, rightOffset, straightOffset, isPawnPinned);
            }
        }
    }

    private void generateEPMove(byte rank, byte file, byte pieceSquare, byte dirOffset, byte straightOffset, boolean isPawnPinned) {
        byte kingRank = (byte)(player.kingSquare / 8);
        byte kingFile = (byte)(player.kingSquare % 8);

        boolean canCapture = !isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, (byte)(pieceSquare + dirOffset));
        canCapture = canCapture && (!isChecked || isBlockingCheck((byte)(pieceSquare + dirOffset - straightOffset)));

        // check for sliding pieces on the left and right
        if (canCapture && kingRank == rank) {
            byte fileItr = 0, delta = 0;
            if (kingFile < file) {
                fileItr = (byte)(kingFile + 1);
                delta = 1;
            } else {
                fileItr = (byte)(kingFile - 1);
                delta = -1;
            }

            boolean playerPawnFound = false;
            boolean enemyPawnFound = false;
            boolean otherPieceFound = false;
            byte slidingPieceFile = -1;

            while (fileItr >= 0 && fileItr < 8 && !otherPieceFound && slidingPieceFile == -1) {
                byte targetSquare = BoardUtil.getSquare(rank, fileItr);

                if (manager.board[targetSquare] != PieceUtil.TYPE_NONE) {
                    byte targetPiece = manager.board[targetSquare];

                    if (PieceUtil.getPieceColor(targetPiece) == player.color) {
                        if (PieceUtil.getPieceType(targetPiece) == PieceUtil.TYPE_PAWN) {
                            if (playerPawnFound) otherPieceFound = true;
                            else playerPawnFound = true;
                        } else {
                            otherPieceFound = true;
                        }
                    } else {
                        if (PieceUtil.isQueenOrRook(targetPiece)) {
                            slidingPieceFile = fileItr;
                        } else if (PieceUtil.getPieceType(targetPiece) == PieceUtil.TYPE_PAWN) {
                            if (enemyPawnFound) otherPieceFound = true;
                            else enemyPawnFound = true;
                        } else {
                            otherPieceFound = true;
                        }
                    }
                }
                fileItr += delta;
            }
            canCapture = slidingPieceFile == -1 || (file < slidingPieceFile && slidingPieceFile < kingFile) || (kingFile < slidingPieceFile && slidingPieceFile < file);
        }

        if (canCapture) {
            legalMoves.add(new Move(pieceSquare, (byte)(pieceSquare + dirOffset), Move.MOVE_EP, 90));
        }
    }
}
