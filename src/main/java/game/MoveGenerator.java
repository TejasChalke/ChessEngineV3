package game;

import util.BoardUtil;
import util.PieceUtil;

import java.util.ArrayList;

public class MoveGenerator {
    private final Manager manager;
    private Player player;
    private Player opponent;

    public boolean isChecked;
    private boolean isDoubleChecked;
    private long attackMask;
    private long pinMask;
    private long checkMask;

    public MoveGenerator(Manager manager) {
        this.manager = manager;
    }

    public ArrayList<Move> getLegalMoves(boolean capturesOnly) {
        player = manager.getPlayer();
        opponent = manager.getOpponent();

        setAttackMask();
        return generateLegalMoves(capturesOnly);
    }

    private ArrayList<Move> generateLegalMoves(boolean capturesOnly) {
        ArrayList<Move> legalMoves = new ArrayList<>();

        // king moves
        for (short offset : BoardUtil.kingOffsets) {
            short targetSquare = (short)(offset + player.kingSquare);
            if (targetSquare < 0 || targetSquare > 63) continue;

            short targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
            if (isSquareSafe(targetSquare) && ((targetPiece == PieceUtil.TYPE_NONE && !capturesOnly) || (targetPiece != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color))) {
                // the square must be safe
                // if an empty square capturesOnly must be false (normal move)
                // if not empty, must be an enemy piece
                int gain = targetPiece == PieceUtil.TYPE_NONE ? 0 : PieceUtil.getPieceValue(targetPiece);
                legalMoves.add(new Move(player.kingSquare, targetSquare, Move.MOVE_DEFAULT, gain));
            }
        }
        if (isDoubleChecked) return legalMoves;

        boolean canCastleKingSide = (player.color == Player.WHITE ? (manager.castleRights & BoardUtil.WHITE_KSC_MASK) : (manager.castleRights & BoardUtil.BLACK_KSC_MASK)) != 0;
        boolean canCastleQueenSide = (player.color == Player.WHITE ? (manager.castleRights & BoardUtil.WHITE_QSC_MASK) : (manager.castleRights & BoardUtil.BLACK_QSC_MASK)) != 0;

        // Castling moves
        if (!isChecked && canCastleKingSide && manager.board[player.kingSquare + 1] == PieceUtil.TYPE_NONE && isSquareSafe((short)(player.kingSquare + 1)) && manager.board[player.kingSquare + 2] == PieceUtil.TYPE_NONE && isSquareSafe((short)(player.kingSquare + 2))) {
            legalMoves.add(new Move(player.kingSquare, (short)(player.kingSquare + 2), Move.MOVE_CKS, 2000));
        }
        try {
            if (!isChecked && canCastleQueenSide && manager.board[player.kingSquare - 1] == PieceUtil.TYPE_NONE && isSquareSafe((short)(player.kingSquare - 1)) && manager.board[player.kingSquare - 2] == PieceUtil.TYPE_NONE && isSquareSafe((short)(player.kingSquare - 2)) && manager.board[player.kingSquare - 3] == PieceUtil.TYPE_NONE) {
                legalMoves.add(new Move(player.kingSquare, (short)(player.kingSquare - 2), Move.MOVE_CQS, 2000));
            }
        } catch (Exception e) {
            System.out.println(player.kingSquare);
        }


        legalMoves.addAll(getSlidingMoves(player.queens, 0, 7, capturesOnly));
        legalMoves.addAll(getSlidingMoves(player.rooks, 0, 3, capturesOnly));
        legalMoves.addAll(getSlidingMoves(player.queens, 4, 7, capturesOnly));
        legalMoves.addAll(getKnightMoves(player.knights, capturesOnly));
        legalMoves.addAll(getPawnMoves(player.pawns, capturesOnly));

        return legalMoves;
    }

    public void setAttackMask() {
        isChecked = isDoubleChecked = false;
        attackMask = pinMask = 0;
        setSlidingAttackMask(opponent.queens, 0, 7);
        setSlidingAttackMask(opponent.rooks, 0, 3);
        setSlidingAttackMask(opponent.bishops, 4, 7);
        setKnightAttackMask(opponent.knights);
        setPawnAttackMask(opponent.pawns);

        // king attack mask
        for (short offset : BoardUtil.kingOffsets) {
            short targetSquare = (short)(offset + opponent.kingSquare);
            if (targetSquare >= 0 && targetSquare < 64) {
                attackMask |= 1L << targetSquare;
            }
        }
    }

    private void setSlidingAttackMask(Pieces pieces, int dirStart, int dirEnd) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            short pieceSquare = pieces.positions[i];
            for (int dir = dirStart; dir <= dirEnd; dir++) {
                short maxMoves = BoardUtil.moveCnt[pieceSquare][dir];
                short currentMoves = 0;

                long currentAttackMask = 0;
                long currentPinMask = 0;

                boolean kingFound = false;
                boolean otherPieceFound = false;

                while (currentMoves++ < maxMoves) {
                    short targetSquare = (short)(pieceSquare + BoardUtil.moveOffsets[dir] * currentMoves);
                    short targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
                    long squareMask = 1L << targetSquare;

                    if (!otherPieceFound) {
                        currentAttackMask |= squareMask;
                    }
                    if (targetPiece != PieceUtil.TYPE_NONE || otherPieceFound || kingFound) {
                        if (targetPiece != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color) {
                            break;
                        }

                        currentPinMask |= squareMask;
                        if (targetPiece == PieceUtil.TYPE_KING) {
                            kingFound = true;
                            if (!otherPieceFound) {
                                if (isChecked) isDoubleChecked = true;
                                else isChecked = true;
                                checkMask |= currentAttackMask;
                            }
                        } else if (targetPiece != PieceUtil.TYPE_NONE) {
                            if (!otherPieceFound) {
                                // first piece found
                                otherPieceFound = true;
                            } else {
                                // second piece found before the king
                                break;
                            }
                        }
                        if (kingFound && otherPieceFound) {
                            break;
                        }
                    }
                }

                if (kingFound) {
                    pinMask |= currentPinMask;
                }
                attackMask |= currentAttackMask;
            }
        }
    }

    private void setKnightAttackMask(Pieces pieces) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            short pieceSquare = pieces.positions[i];
            for (short targetSquare : BoardUtil.KNIGHT_MOVES[pieceSquare]) {
                attackMask |= 1L << targetSquare;

                if (PieceUtil.getPieceType(manager.board[targetSquare]) == PieceUtil.TYPE_KING && PieceUtil.getPieceColor(manager.board[targetSquare]) == player.color) {
                    if (isChecked) isDoubleChecked = true;
                    else isChecked = true;
                    checkMask |= 1L << targetSquare;
                }
            }
        }
    }

    private void setPawnAttackMask(Pieces pieces) {
        for (int i = 0; i < pieces.currentCnt; i++) {
            short pieceSquare = pieces.positions[i];
            boolean canAttackLeft = pieceSquare % 8 != 0;
            boolean canAttackRight = pieceSquare % 7 != 0;

            if (canAttackLeft) {
                int targetSquare = opponent.color == Player.WHITE ? pieceSquare + 7 : pieceSquare - 9;
                attackMask |= 1L << targetSquare;

                if (PieceUtil.getPieceType(manager.board[targetSquare]) == PieceUtil.TYPE_KING && PieceUtil.getPieceColor(manager.board[targetSquare]) == player.color) {
                    if (isChecked) isDoubleChecked = true;
                    else isChecked = true;
                    checkMask |= 1L << targetSquare;
                }
            }
            if (canAttackRight) {
                int targetSquare = opponent.color == Player.WHITE ? pieceSquare + 9 : pieceSquare - 7;
                attackMask |= 1L << targetSquare;

                if (PieceUtil.getPieceType(manager.board[targetSquare]) == PieceUtil.TYPE_KING && PieceUtil.getPieceColor(manager.board[targetSquare]) == player.color) {
                    if (isChecked) isDoubleChecked = true;
                    else isChecked = true;
                    checkMask |= 1L << targetSquare;
                }
            }
        }
    }

    private boolean isSquareSafe(short square) {
        return (square & attackMask) != 0;
    }

    private boolean isSquarePinned(short square) {
        return (square & pinMask) != 0;
    }

    private boolean isBlockingCheck(short square) {
        return (square & checkMask) != 0;
    }

    private ArrayList<Move> getSlidingMoves(Pieces pieces, int dirStart, int dirEnd, boolean capturesOnly) {
        ArrayList<Move> legalMoves = new ArrayList<>();
//        if (!manager.whiteToMove) {
//            BoardUtil.displayBoard(manager.board);
//        }
        for (int i = 0; i < pieces.currentCnt; i++) {
            short pieceSquare = pieces.positions[i];
            for (int dir = dirStart; dir <= dirEnd; dir++) {
                short maxMoves = BoardUtil.moveCnt[pieceSquare][dir];
                if (maxMoves > 0 && isSquarePinned(pieceSquare) && !BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, pieceSquare + BoardUtil.moveOffsets[dir])) {
                    // the pinned piece must move in the direction of the pin
                    break;
                }

                short currentMoves = 0;
                while (currentMoves++ < maxMoves) {
                    short targetSquare = (short)(pieceSquare + BoardUtil.moveOffsets[dir] * currentMoves);
                    short targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);
                    short targetPieceColor = PieceUtil.getPieceColor(manager.board[targetSquare]);

                    if (targetPiece != PieceUtil.TYPE_NONE && targetPieceColor == player.color) {
                        break;
                    }
                    if (((targetPiece == PieceUtil.TYPE_NONE && !capturesOnly) || (targetPiece != PieceUtil.TYPE_NONE && targetPieceColor == opponent.color))
                            && (!isChecked || isBlockingCheck(targetSquare))) {
                        int gain = targetPiece == PieceUtil.TYPE_NONE ? 0 : PieceUtil.getPieceValue(targetPiece) - PieceUtil.getPieceValue(manager.board[pieceSquare]) / 10;
                        legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, gain));
                    }
                }
            }
        }
        return legalMoves;
    }

    private ArrayList<Move> getKnightMoves(Pieces pieces, boolean capturesOnly) {
        ArrayList<Move> legalMoves = new ArrayList<>();
        for (int i = 0; i < pieces.currentCnt; i++) {
            short pieceSquare = pieces.positions[i];

            for (short targetSquare : BoardUtil.KNIGHT_MOVES[pieceSquare]) {
                short targetPiece = PieceUtil.getPieceType(manager.board[targetSquare]);

                if (((targetPiece == PieceUtil.TYPE_NONE && !capturesOnly) || (targetPiece != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[targetSquare]) == opponent.color))
                        && (!isChecked || isBlockingCheck(targetSquare))) {
                    int gain = targetPiece == PieceUtil.TYPE_NONE ? 0 : PieceUtil.getPieceValue(targetPiece) - PieceUtil.KNIGHT_VALUE / 10;
                    legalMoves.add(new Move(pieceSquare, targetSquare, Move.MOVE_DEFAULT, gain));
                }
            }
        }
        return legalMoves;
    }

    private ArrayList<Move> getPawnMoves(Pieces pieces, boolean capturesOnly) {
        ArrayList<Move> legalMoves = new ArrayList<>();
        for (int i = 0; i < pieces.currentCnt; i++) {
            short pieceSquare = pieces.positions[i];
            short rank = (short)(pieceSquare / 8);
            short file = (short)(pieceSquare % 8);
            boolean isPawnPinned = isSquarePinned(pieceSquare);
            short leftOffset, rightOffset, straightOffset, promotionRank, twoSquaresAheadRank;

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
            if (manager.board[pieceSquare + straightOffset] == PieceUtil.TYPE_NONE && !capturesOnly && (!isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, pieceSquare + straightOffset))) {
                if (!isChecked || isBlockingCheck((short)(pieceSquare + straightOffset))) {
                    if (rank == promotionRank) {
                        // promotion moves
                        legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + straightOffset), Move.MOVE_P_QUEEN, PieceUtil.QUEEN_VALUE));
                        legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + straightOffset), Move.MOVE_P_ROOK, PieceUtil.ROOK_VALUE));
                        legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + straightOffset), Move.MOVE_P_BISHOP, PieceUtil.BISHOP_VALUE));
                        legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + straightOffset), Move.MOVE_P_KNIGHT, PieceUtil.KNIGHT_VALUE));
                    } else {
                        legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + straightOffset), Move.MOVE_DEFAULT, 0));
                    }
                }

                if (rank == twoSquaresAheadRank && manager.board[(short)(pieceSquare + straightOffset * 2)] == PieceUtil.TYPE_NONE
                        && (!isChecked || isBlockingCheck((short)(pieceSquare + straightOffset * 2)))) {
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + straightOffset * 2), Move.MOVE_2_SQUARES, 0));
                }
            }

            // capture piece left
            if (file > 0 && manager.board[pieceSquare + leftOffset] != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[pieceSquare + leftOffset]) == opponent.color
                    && (!isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, pieceSquare + leftOffset))
                    && (!isChecked || isBlockingCheck((short)(pieceSquare + leftOffset)))) {
                int opponentPieceValue = PieceUtil.getPieceValue(manager.board[pieceSquare + leftOffset]);
                if (rank == promotionRank) {
                    // promotion moves
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + leftOffset), Move.MOVE_P_QUEEN, PieceUtil.QUEEN_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + leftOffset), Move.MOVE_P_ROOK, PieceUtil.ROOK_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + leftOffset), Move.MOVE_P_BISHOP, PieceUtil.BISHOP_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + leftOffset), Move.MOVE_P_KNIGHT, PieceUtil.KNIGHT_VALUE + opponentPieceValue));
                } else {
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + leftOffset), Move.MOVE_DEFAULT, opponentPieceValue));
                }
            }

            // capture piece right
            if (file < 7 && manager.board[pieceSquare + rightOffset] != PieceUtil.TYPE_NONE && PieceUtil.getPieceColor(manager.board[pieceSquare + rightOffset]) == opponent.color
                    && (!isPawnPinned || BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, pieceSquare + rightOffset))
                    && (!isChecked || isBlockingCheck((short)(pieceSquare + rightOffset)))) {
                int opponentPieceValue = PieceUtil.getPieceValue(manager.board[pieceSquare + rightOffset]);
                if (rank == promotionRank) {
                    // promotion moves
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + rightOffset), Move.MOVE_P_QUEEN, PieceUtil.QUEEN_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + rightOffset), Move.MOVE_P_ROOK, PieceUtil.ROOK_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + rightOffset), Move.MOVE_P_BISHOP, PieceUtil.BISHOP_VALUE + opponentPieceValue));
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + rightOffset), Move.MOVE_P_KNIGHT, PieceUtil.KNIGHT_VALUE + opponentPieceValue));
                } else {
                    legalMoves.add(new Move(pieceSquare, (short)(pieceSquare + rightOffset), Move.MOVE_DEFAULT, opponentPieceValue));
                }
            }

            // capture en passant
            if (file > 0 && manager.epSquare == pieceSquare + leftOffset) {
                Move epMove = getEpMove(rank, file, pieceSquare, leftOffset, straightOffset, isPawnPinned);
                if (epMove != null) legalMoves.add(epMove);
            } else if (file < 7 && manager.epSquare == pieceSquare + rightOffset) {
                Move epMove = getEpMove(rank, file, pieceSquare, rightOffset, straightOffset, isPawnPinned);
                if (epMove != null) legalMoves.add(epMove);
            }
        }
        return legalMoves;
    }

    private Move getEpMove(short rank, short file, short pieceSquare, short dirOffset, short straightOffset, boolean isPawnPinned) {
        short kingRank = (short)(player.kingSquare / 8);
        short kingFile = (short)(player.kingSquare % 8);
        boolean canCapture = true;

        if (isPawnPinned) canCapture = BoardUtil.isMovingOnThePinLine(player.kingSquare, pieceSquare, (short)(pieceSquare + dirOffset));
        canCapture = canCapture && (!isChecked || isBlockingCheck((short)(pieceSquare + dirOffset - straightOffset)));

        // check for sliding pieces on the left and right
        if (canCapture && kingRank == rank) {
            int fileItr = 0, delta = 0;
            if (kingFile < file) {
                fileItr = kingFile + 1;
                delta = 1;
            } else {
                fileItr = kingFile - 1;
                delta = -1;
            }

            boolean otherPieceFound = false;
            boolean playerPawnFound = false;
            boolean enemyPawnFound = false;
            boolean slidingPieceFound = false;

            while (fileItr >= 0 && fileItr < 8 && !otherPieceFound && !slidingPieceFound) {
                short targetSquare = BoardUtil.getSquare(rank, (short)fileItr);
                if (manager.board[targetSquare] != PieceUtil.TYPE_NONE) {
                    short targetPiece = manager.board[targetSquare];

                    if (PieceUtil.getPieceColor(manager.board[targetSquare]) == player.color) {
                        if (PieceUtil.getPieceType(targetPiece) == PieceUtil.TYPE_PAWN) {
                            if (playerPawnFound) otherPieceFound = true;
                            else playerPawnFound = true;
                        } else {
                            otherPieceFound = true;
                        }
                    } else {
                        if (PieceUtil.isQueenOrRook(targetPiece)) {
                            slidingPieceFound = true;
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
            canCapture = !slidingPieceFound;
        }
        return canCapture ? new Move(pieceSquare, (short)(pieceSquare + dirOffset), Move.MOVE_EP, 90) : null;
    }
}
