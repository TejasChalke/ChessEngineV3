package game;

import util.BoardUtil;
import util.PieceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class Engine {
    private final Manager manager;
    private final MoveGenerator generator;
    private final Evaluator evaluator;
    private Move bestMove;
    private int currentMaxDepth;
    Stack<MoveInfo> previousMoves;
    private int positionsReached;
    private int enPassants;
    private int captures;

    private final int MAX_EVAL = (int)1e7;

    public Engine(Manager manager) {
        bestMove = null;
        this.manager = manager;
        generator = new MoveGenerator(manager);
        evaluator = new Evaluator(manager);
        currentMaxDepth = 0;
        previousMoves = new Stack<>();
    }

    public Move getBestMove() {
        // TODO: implement iterative deepening
        int eval = findBestMove(4, 0);
        if (bestMove == null) {
            if (!generator.isChecked) bestMove = new Move((short)-3, (short)0, (short)0, 0);
            else bestMove = new Move((short)(manager.whiteToMove ? -2 : -1), (short)0, (short)0, 0);
        }
        return bestMove;
    }

    public void test(int depth) {
        positionsReached = enPassants = captures = 0;
        findBestMove(depth, 0);
        System.out.println("At depth " + depth + ", number of positions reached : " + positionsReached);
        System.out.println("En-passants : " + enPassants + ", Captures : " + captures);
    }

    public int findBestMove(int depth, int depthFromRoot) {
        if (depth == 0) {
            positionsReached++;
//            BoardUtil.displayBoard(manager.board);
            return evaluator.evaluate();
        }

        // TODO: 50 move rule
        if (manager.halfMoveClock == 50) return 0;

        ArrayList<Move> legaMoves = generator.getLegalMoves(false);
        // checkmate or stalemate
        if (legaMoves.isEmpty()) return generator.isChecked ? -(MAX_EVAL - depthFromRoot) : 0;
        Collections.sort(legaMoves);

        int currentBestEval = manager.whiteToMove ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : legaMoves) {
            makeMove(move, depthFromRoot + 1);
            int currentEval = findBestMove(depth - 1, depthFromRoot + 1);
            if (manager.whiteToMove && currentEval > currentBestEval) {
                currentBestEval = currentEval;
                if (depthFromRoot == 0) {
                    bestMove = move;
                }
            } else if (!manager.whiteToMove && currentEval < currentBestEval) {
                currentBestEval = currentEval;
                if (depthFromRoot == 0) {
                    bestMove = move;
                }
            }
            unMakeMove(move);
        }
        return currentBestEval;
    }

    public void makeMove(Move move, int movesPlayed) {
        MoveInfo moveInfo = new MoveInfo(manager.epSquare, manager.castleRights);
        manager.epSquare = -1;

        if (Move.isPromotionMove(move.moveType)) {
            short targetPiece = manager.board[move.targetSquare];
            if (targetPiece != PieceUtil.TYPE_NONE) {
                manager.getOpponent().getPieces(targetPiece).removePiece(move.targetSquare);
                captures++;
            }

            short newPiece = (short)(PieceUtil.getPromotionPiece(move.moveType) | manager.getPlayer().color);
            manager.board[move.targetSquare] = newPiece;
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;

            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).removePiece(move.startSquare);
            manager.getPlayer().getPieces(newPiece).addPiece(move.targetSquare);
        } else if (Move.MOVE_CKS == move.moveType) {
            short kingSquare = manager.getPlayer().kingSquare;
            manager.board[kingSquare + 1] = manager.board[kingSquare + 3];
            manager.board[kingSquare + 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare + 3] = PieceUtil.TYPE_NONE;

            manager.removePlayerCastleRights();
            manager.getPlayer().kingSquare = move.targetSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((short)(move.targetSquare + 1), (short)(move.targetSquare - 1));
        } else if (Move.MOVE_CQS == move.moveType) {
            short kingSquare = manager.getPlayer().kingSquare;
            manager.board[kingSquare - 1] = manager.board[kingSquare - 4];
            manager.board[kingSquare - 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare - 4] = PieceUtil.TYPE_NONE;

            manager.removePlayerCastleRights();
            manager.getPlayer().kingSquare = move.targetSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((short)(move.targetSquare - 2), (short)(move.targetSquare + 1));
        } else if (Move.MOVE_2_SQUARES == move.moveType) {
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;
            manager.epSquare = (short)(move.targetSquare + (manager.whiteToMove ? -8 : 8));
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.startSquare, move.targetSquare);
        } else if (Move.MOVE_EP == move.moveType) {
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;

            moveInfo.piece = manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)];
            manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)] = PieceUtil.TYPE_NONE;

            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.startSquare, move.targetSquare);
            manager.getOpponent().getPieces(PieceUtil.TYPE_PAWN).removePiece((short)(move.targetSquare + (manager.whiteToMove ? -8 : 8)));
            enPassants++;
        } else {
            short targetPiece = manager.board[move.targetSquare];
            if (targetPiece != PieceUtil.TYPE_NONE) {
                moveInfo.piece = targetPiece;
                manager.getOpponent().getPieces(targetPiece).removePiece(move.targetSquare);
                captures++;
            }

            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;

            if (move.startSquare != manager.getPlayer().kingSquare) {
                manager.getPlayer().getPieces(manager.board[move.targetSquare]).updatePosition(move.startSquare, move.targetSquare);

                if (PieceUtil.getPieceType(manager.board[move.targetSquare]) == PieceUtil.TYPE_ROOK) {
                    if (manager.whiteToMove) {
                        if (move.startSquare == BoardUtil.WHITE_KS_ROOK) {
                            manager.castleRights &= (short)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_KSC_MASK);
                        } else if (move.startSquare == BoardUtil.WHITE_QS_ROOK) {
                            manager.castleRights &= (short)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_QSC_MASK);
                        }
                    } else {
                        if (move.startSquare == BoardUtil.BLACK_KS_ROOK) {
                            manager.castleRights &= (short)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_KSC_MASK);
                        } else if (move.startSquare == BoardUtil.BLACK_QS_ROOK) {
                            manager.castleRights &= (short)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_QSC_MASK);
                        }
                    }
                }
            } else {
                manager.removePlayerCastleRights();
                manager.getPlayer().kingSquare = move.targetSquare;
            }
        }

        manager.whiteToMove = !manager.whiteToMove;
        previousMoves.push(moveInfo);
    }

    private void unMakeMove(Move move) {
        MoveInfo moveInfo = previousMoves.pop();
        manager.whiteToMove = !manager.whiteToMove;
        manager.epSquare = moveInfo.epSquare;
        manager.castleRights = moveInfo.castleRights;

        if (Move.isPromotionMove(move.moveType)) {
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).addPiece(move.startSquare);
            manager.getPlayer().getPieces(manager.board[move.targetSquare]).removePiece(move.targetSquare);
            if (moveInfo.piece != PieceUtil.TYPE_NONE) {
                manager.getOpponent().getPieces(moveInfo.piece).addPiece(move.targetSquare);
            }

            manager.board[move.startSquare] = (short)(PieceUtil.TYPE_PAWN | manager.getPlayer().color);
            manager.board[move.targetSquare] = moveInfo.piece;
        } else if (Move.MOVE_CKS == move.moveType) {
            short kingSquare = manager.getPlayer().kingSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((short)(kingSquare - 1), (short)(kingSquare + 1));

            manager.board[kingSquare + 1] = manager.board[kingSquare - 1];
            manager.board[kingSquare - 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare - 1] = PieceUtil.TYPE_NONE;
            manager.getPlayer().kingSquare = move.startSquare;
        } else if (Move.MOVE_CQS == move.moveType) {
            short kingSquare = manager.getPlayer().kingSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((short)(kingSquare + 1), (short)(kingSquare - 2));

            manager.board[kingSquare - 2] = manager.board[kingSquare + 1];
            manager.board[kingSquare + 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare + 1] = PieceUtil.TYPE_NONE;
            manager.getPlayer().kingSquare = move.startSquare;
        } else if (Move.MOVE_2_SQUARES == move.moveType) {
            manager.board[move.startSquare] = manager.board[move.targetSquare];
            manager.board[move.targetSquare] = PieceUtil.TYPE_NONE;
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.targetSquare, move.startSquare);
        } else if (Move.MOVE_EP == move.moveType) {
            manager.board[move.startSquare] = manager.board[move.targetSquare];
            manager.board[move.targetSquare] = PieceUtil.TYPE_NONE;

            manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)] = moveInfo.piece;
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.targetSquare, move.startSquare);
            manager.getOpponent().getPieces(PieceUtil.TYPE_PAWN).addPiece((short)(move.targetSquare + (manager.whiteToMove ? -8 : 8)));
        } else {
            if (move.targetSquare != manager.getPlayer().kingSquare) {
                manager.getPlayer().getPieces(manager.board[move.targetSquare]).updatePosition(move.targetSquare, move.startSquare);
            } else {
                manager.getPlayer().kingSquare = move.startSquare;
            }

            if (moveInfo.piece != PieceUtil.TYPE_NONE) {
                manager.getOpponent().getPieces(moveInfo.piece).addPiece(move.targetSquare);
            }
            manager.board[move.startSquare] = manager.board[move.targetSquare];
            manager.board[move.targetSquare] = moveInfo.piece;
        }
    }
}
