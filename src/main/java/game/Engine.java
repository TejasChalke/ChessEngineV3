package game;

import util.BoardUtil;
import util.PieceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

public class Engine {
    private final Manager manager;
    private final MoveGenerator generator;
    private final Evaluator evaluator;
    private Move bestMove;
    private int currentMaxDepth;
    Stack<MoveInfo> previousMoves;

    private final int MAX_EVAL = (int)1e9;
    private final int CHECKMATE_EVAL = (int)1e7;

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
        bestMove = null;
        findBestMove(4, 0, -MAX_EVAL, MAX_EVAL);
        if (bestMove == null) {
            if (!generator.isChecked) bestMove = new Move((byte)-3, (byte)0, (byte)0, 0);
            else bestMove = new Move((byte)(manager.whiteToMove ? -2 : -1), (byte)0, (byte)0, 0);
        }
        return bestMove;
    }

    public ArrayList<Move> getLegalMoves() {
        return generator.getLegalMoves(false);
    }

    public int findBestMove(int depth, int depthFromRoot, int alpha, int beta) {
        if (depth == 0) {
            return findQuitePosition(alpha, beta);
        }

        // 50 move rule
        if (manager.halfMoveClock == 50) return 0;
        ArrayList<Move> legaMoves = generator.getLegalMoves(false);

        // checkmate or stalemate
        if (legaMoves.isEmpty()) return generator.isChecked ? -(CHECKMATE_EVAL - depthFromRoot) : 0;
        Collections.sort(legaMoves);

        for (Move move : legaMoves) {
            makeMove(move);
            int eval = -findBestMove(depth - 1, depthFromRoot + 1, -beta, -alpha);
            unMakeMove(move);

            if (beta <= alpha) {
                return beta;
            }
            if (alpha < eval) {
                alpha = eval;
                if (depthFromRoot == 0) {
                    bestMove = move;
                }
            }
        }
        return alpha;
    }

    public int findQuitePosition(int alpha, int beta) {
        int eval = evaluator.evaluate();
        if (eval >= beta) {
            return beta;
        } else if (alpha < eval) {
            alpha = eval;
        }

        ArrayList<Move> legaMoves = generator.getLegalMoves(true);
        Collections.sort(legaMoves);

        for (Move move : legaMoves) {
            makeMove(move);
            eval = -findQuitePosition(-beta, -alpha);
            unMakeMove(move);

            if (beta <= alpha) {
                return beta;
            }
            if (alpha < eval) {
                alpha = eval;
            }
        }
        return alpha;
    }

    public void makeMove(Move move) {
        MoveInfo moveInfo = new MoveInfo(manager.epSquare, manager.castleRights, manager.halfMoveClock);
        manager.epSquare = -1;

        if (Move.isPromotionMove(move.moveType)) {
            byte targetPiece = manager.board[move.targetSquare];
            if (targetPiece != PieceUtil.TYPE_NONE) {
                moveInfo.piece = targetPiece;
                manager.getOpponent().getPieces(targetPiece).removePiece(move.targetSquare);
            }

            if (manager.castleRights != 0) {
                if (move.targetSquare == BoardUtil.WHITE_KS_ROOK) {
                    manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_KSC_MASK);
                } else if (move.targetSquare == BoardUtil.WHITE_QS_ROOK) {
                    manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_QSC_MASK);
                } else if (move.targetSquare == BoardUtil.BLACK_KS_ROOK) {
                    manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_KSC_MASK);
                } else if (move.targetSquare == BoardUtil.BLACK_QS_ROOK) {
                    manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_QSC_MASK);
                }
            }

            byte newPiece = (byte)(PieceUtil.getPromotionPiece(move.moveType) | manager.getPlayer().color);
            manager.board[move.targetSquare] = newPiece;
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;

            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).removePiece(move.startSquare);
            manager.getPlayer().getPieces(newPiece).addPiece(move.targetSquare);
            manager.halfMoveClock = 0;
        } else if (Move.MOVE_CKS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.board[kingSquare + 1] = manager.board[kingSquare + 3];
            manager.board[kingSquare + 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare + 3] = PieceUtil.TYPE_NONE;

            manager.removePlayerCastleRights();
            manager.getPlayer().kingSquare = move.targetSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(move.targetSquare + 1), (byte)(move.targetSquare - 1));
            manager.halfMoveClock++;
        } else if (Move.MOVE_CQS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.board[kingSquare - 1] = manager.board[kingSquare - 4];
            manager.board[kingSquare - 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare - 4] = PieceUtil.TYPE_NONE;

            manager.removePlayerCastleRights();
            manager.getPlayer().kingSquare = move.targetSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(move.targetSquare - 2), (byte)(move.targetSquare + 1));
            manager.halfMoveClock++;
        } else if (Move.MOVE_2_SQUARES == move.moveType) {
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;
            manager.epSquare = (byte)(move.targetSquare + (manager.whiteToMove ? -8 : 8));
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.startSquare, move.targetSquare);
            manager.halfMoveClock = 0;
        } else if (Move.MOVE_EP == move.moveType) {
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;

            moveInfo.piece = manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)];
            manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)] = PieceUtil.TYPE_NONE;

            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.startSquare, move.targetSquare);
            manager.getOpponent().getPieces(PieceUtil.TYPE_PAWN).removePiece((byte)(move.targetSquare + (manager.whiteToMove ? -8 : 8)));
            manager.halfMoveClock = 0;
        } else {
            byte targetPiece = manager.board[move.targetSquare];
            byte startingPiece = PieceUtil.getPieceType(manager.board[move.startSquare]);

            if (targetPiece != PieceUtil.TYPE_NONE) {
                moveInfo.piece = targetPiece;
                manager.getOpponent().getPieces(targetPiece).removePiece(move.targetSquare);

                if (manager.castleRights != 0 && PieceUtil.TYPE_ROOK == PieceUtil.getPieceType(targetPiece)) {
                    if (move.targetSquare == BoardUtil.WHITE_KS_ROOK) {
                        manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_KSC_MASK);
                    } else if (move.targetSquare == BoardUtil.WHITE_QS_ROOK) {
                        manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_QSC_MASK);
                    } else if (move.targetSquare == BoardUtil.BLACK_KS_ROOK) {
                        manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_KSC_MASK);
                    } else if (move.targetSquare == BoardUtil.BLACK_QS_ROOK) {
                        manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_QSC_MASK);
                    }
                }
                manager.halfMoveClock = 0;
            } else {
                if (startingPiece == PieceUtil.TYPE_PAWN) manager.halfMoveClock = 0;
                else manager.halfMoveClock++;
            }

            if (move.startSquare != manager.getPlayer().kingSquare) {
                if (manager.castleRights != 0) {
                    if (startingPiece == PieceUtil.TYPE_ROOK) {
                        if (manager.whiteToMove) {
                            if (move.startSquare == BoardUtil.WHITE_KS_ROOK) {
                                manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_KSC_MASK);
                            } else if (move.startSquare == BoardUtil.WHITE_QS_ROOK) {
                                manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.WHITE_QSC_MASK);
                            }
                        } else {
                            if (move.startSquare == BoardUtil.BLACK_KS_ROOK) {
                                manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_KSC_MASK);
                            } else if (move.startSquare == BoardUtil.BLACK_QS_ROOK) {
                                manager.castleRights &= (byte)(BoardUtil.CASTLE_MASK ^ BoardUtil.BLACK_QSC_MASK);
                            }
                        }
                    }
                }

                manager.getPlayer().getPieces(manager.board[move.startSquare]).updatePosition(move.startSquare, move.targetSquare);
            } else {
                manager.removePlayerCastleRights();
                manager.getPlayer().kingSquare = move.targetSquare;
            }

            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;
        }

        manager.whiteToMove = !manager.whiteToMove;
        previousMoves.push(moveInfo);
    }

    private void unMakeMove(Move move) {
        MoveInfo moveInfo = previousMoves.pop();
        manager.whiteToMove = !manager.whiteToMove;
        manager.epSquare = moveInfo.epSquare;
        manager.castleRights = moveInfo.castleRights;
        manager.halfMoveClock = moveInfo.halfMoveClock;

        if (Move.isPromotionMove(move.moveType)) {
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).addPiece(move.startSquare);
            manager.getPlayer().getPieces(manager.board[move.targetSquare]).removePiece(move.targetSquare);
            if (moveInfo.piece != PieceUtil.TYPE_NONE) {
                manager.getOpponent().getPieces(moveInfo.piece).addPiece(move.targetSquare);
            }

            manager.board[move.startSquare] = (byte)(PieceUtil.TYPE_PAWN | manager.getPlayer().color);
            manager.board[move.targetSquare] = moveInfo.piece;
        } else if (Move.MOVE_CKS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(kingSquare - 1), (byte)(kingSquare + 1));

            manager.board[kingSquare + 1] = manager.board[kingSquare - 1];
            manager.board[kingSquare - 2] = manager.board[kingSquare];
            manager.board[kingSquare] = manager.board[kingSquare - 1] = PieceUtil.TYPE_NONE;
            manager.getPlayer().kingSquare = move.startSquare;
        } else if (Move.MOVE_CQS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(kingSquare + 1), (byte)(kingSquare - 2));

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
            manager.getOpponent().getPieces(PieceUtil.TYPE_PAWN).addPiece((byte)(move.targetSquare + (manager.whiteToMove ? -8 : 8)));
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

    public long test(int depth) {
        return getNodeCount(depth);
    }

    private long getNodeCount(int depth) {
        if (depth == 0) {
            return 1;
        }
        ArrayList<Move> legaMoves = generator.getLegalMoves(false);
        long moves = 0;
        for (Move move : legaMoves) {
            makeMove(move);
            long currentMoves = getNodeCount(depth - 1);
            moves += currentMoves;
            unMakeMove(move);
        }
        return moves;
    }
}
