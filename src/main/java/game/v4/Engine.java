package game.v4;

import util.BoardUtil;
import util.PieceUtil;

import java.util.*;

public class Engine {
    private final Manager manager;
    private final MoveGenerator generator;
    public TranspositionTable table;
    public Zobrist zobrist;
    private final Evaluator evaluator;
    public Stack<MoveInfo> previousMoves;
    public HashMap<Long, Integer> repeatedPositions;

    private final int MAX_EVAL = (int)1e8;
    private final int CHECKMATE_EVAL = (int)1e6;

    private Move bestMove;
    private Move bestMoveThisIteration;
    private int bestEvalThisIteration;

    private boolean searchCancelled;
    private long searchEndTime;
    private final long searchTimeAllowed = 1000;

    public Engine(Manager manager) {
        bestMove = null;
        this.manager = manager;
        table = new TranspositionTable(1 << 20);
        zobrist = new Zobrist(manager.board, manager.castleRights, manager.epSquare);
        generator = new MoveGenerator(manager);
        evaluator = new Evaluator(manager);
        previousMoves = new Stack<>();
        repeatedPositions = new HashMap<>();
    }

    public Move getBestMove() {
        bestMove = null;
        searchCancelled = false;
        searchEndTime = searchTimeAllowed + System.currentTimeMillis();
        table.clear();

        int currentDepth = 1, bestEval = 0;
        while (!searchCancelled) {
            bestEvalThisIteration = 0;
            bestMoveThisIteration = null;

            findBestMove(currentDepth++, 0, -MAX_EVAL, MAX_EVAL);
            if (searchCancelled) break;

            bestEval = bestEvalThisIteration;
            if (bestMoveThisIteration != null) {
                bestMove = bestMoveThisIteration;
            }
        }

//        if (bestMove != null)
//            System.out.println("For depth " + (currentDepth - 1) + " best move for " + manager.getPlayer().getName() + " (" + BoardUtil.getUCINotation(bestMove) + ") with eval " + bestEval);

        if (bestMove == null) {
            if (!generator.isChecked) bestMove = new Move((byte)-3, (byte)0, (byte)0, 0);
            else bestMove = new Move((byte)(manager.whiteToMove ? -2 : -1), (byte)0, (byte)0, 0);
        }
        return bestMove;
    }

    public ArrayList<Move> getLegalMoves() {
        return generator.getLegalMoves(null, false);
    }

    public int findBestMove(int depth, int depthFromRoot, int alpha, int beta) {
        if (depth == 0) {
            return findQuitePosition(alpha, beta);
        }

        // This will add the best move from previous depth's search
        ArrayList<Move> legaMoves = generator.getLegalMoves(depthFromRoot == 0 ? bestMove : null, false);
        // checkmate or stalemate
        if (legaMoves.isEmpty()) {
            updateSearchCancelled();
            return generator.isChecked ? -(CHECKMATE_EVAL - depthFromRoot) : 0;
        }

        // 50 move rule or search time over
        if (manager.halfMoveClock == 50 || searchCancelled || updateSearchCancelled()) {
            return 0;
        }

        long currentPos = zobrist.hash;
        if (repeatedPositions.containsKey(currentPos)) {
            int cnt = repeatedPositions.get(currentPos);
            if (cnt == 3) {
                return 0; // draw by repetition
            }
            else repeatedPositions.put(currentPos, cnt + 1);
        } else {
            repeatedPositions.put(currentPos, 1);
        }

        if (table.hasEntry(zobrist.hash, depth)) {
            int ttEval = table.getEval(alpha, beta);
            if (ttEval != TranspositionTable.lookUpFailedValue) {
                if (depthFromRoot == 0) {
                    bestEvalThisIteration = ttEval;
                    bestMoveThisIteration = table.getMove();
                }
                repeatedPositions.put(currentPos, repeatedPositions.get(currentPos) - 1);
                return ttEval;
            }
        }

        Collections.sort(legaMoves);
        Move bestMoveThisDepth = null;
        byte entryType = Entry.TYPE_UPPER_BOUND;
        for (Move move : legaMoves) {
            makeMove(move);
            int eval = -findBestMove(depth - 1, depthFromRoot + 1, -beta, -alpha);
            unMakeMove(move);

            if (searchCancelled) {
                repeatedPositions.put(currentPos, repeatedPositions.get(currentPos) - 1);
                return 0;
            }

            if (beta <= eval) {
                table.putEntry(zobrist.hash, Entry.TYPE_LOWER_BOUND, depth, beta, move);
                repeatedPositions.put(currentPos, repeatedPositions.get(currentPos) - 1);
                return beta;
            } else if (alpha < eval) {
                alpha = eval;
                bestMoveThisDepth = move;
                entryType = Entry.TYPE_EXACT;
                if (depthFromRoot == 0) {
                    bestEvalThisIteration = eval;
                    bestMoveThisIteration = move;
                }
            }
        }

        repeatedPositions.put(currentPos, repeatedPositions.get(currentPos) - 1);
        table.putEntry(zobrist.hash, entryType, depth, alpha, bestMoveThisDepth);
        return alpha;
    }

    public int findQuitePosition(int alpha, int beta) {
        int eval = evaluator.evaluate();
        if (beta < eval) {
            return beta;
        } else if (alpha < eval) {
            alpha = eval;
        }

        ArrayList<Move> legaMoves = generator.getLegalMoves(null, true);
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
        if (manager.epSquare != -1) {
            zobrist.updateEPHash(manager.epSquare);
            manager.epSquare = -1;
        }
        if (manager.castleRights != 0) {
            zobrist.updateCastleHash(manager.castleRights);
        }

        if (Move.isPromotionMove(move.moveType)) {
            byte targetPiece = manager.board[move.targetSquare];
            if (targetPiece != PieceUtil.TYPE_NONE) {
                moveInfo.piece = targetPiece;
                manager.getOpponent().getPieces(targetPiece).removePiece(move.targetSquare);
                zobrist.updateMoveHash(targetPiece, move.targetSquare, -1);
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
            zobrist.updateMoveHash(newPiece, move.targetSquare, -1);
            manager.board[move.targetSquare] = newPiece;

            zobrist.updateMoveHash(manager.board[move.startSquare], move.startSquare, -1);
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;

            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).removePiece(move.startSquare);
            manager.getPlayer().getPieces(newPiece).addPiece(move.targetSquare);
            manager.halfMoveClock = 0;
        } else if (Move.MOVE_CKS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.board[kingSquare + 1] = manager.board[kingSquare + 3];
            zobrist.updateMoveHash(manager.board[kingSquare + 1], kingSquare + 3, kingSquare + 1);

            manager.board[kingSquare + 2] = manager.board[kingSquare];
            zobrist.updateMoveHash(manager.board[kingSquare + 2], kingSquare, kingSquare + 2);

            manager.board[kingSquare] = manager.board[kingSquare + 3] = PieceUtil.TYPE_NONE;
            manager.removePlayerCastleRights();

            manager.getPlayer().kingSquare = move.targetSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(move.targetSquare + 1), (byte)(move.targetSquare - 1));
            manager.halfMoveClock++;
        } else if (Move.MOVE_CQS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.board[kingSquare - 1] = manager.board[kingSquare - 4];
            zobrist.updateMoveHash(manager.board[kingSquare - 1], kingSquare - 4, kingSquare - 1);

            manager.board[kingSquare - 2] = manager.board[kingSquare];
            zobrist.updateMoveHash(manager.board[kingSquare - 2], kingSquare, kingSquare - 2);

            manager.board[kingSquare] = manager.board[kingSquare - 4] = PieceUtil.TYPE_NONE;
            manager.removePlayerCastleRights();

            manager.getPlayer().kingSquare = move.targetSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(move.targetSquare - 2), (byte)(move.targetSquare + 1));
            manager.halfMoveClock++;
        } else if (Move.MOVE_2_SQUARES == move.moveType) {
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            zobrist.updateMoveHash(manager.board[move.startSquare], move.startSquare, move.targetSquare);

            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;
            manager.epSquare = (byte)(move.targetSquare + (manager.whiteToMove ? -8 : 8));
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.startSquare, move.targetSquare);
            manager.halfMoveClock = 0;
            zobrist.updateEPHash(move.targetSquare);
        } else if (Move.MOVE_EP == move.moveType) {
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            zobrist.updateMoveHash(manager.board[move.startSquare], move.startSquare, move.targetSquare);

            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;
            moveInfo.piece = manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)];
            zobrist.updateMoveHash(moveInfo.piece, move.targetSquare + (manager.whiteToMove ? -8 : 8), -1);
            manager.board[move.targetSquare + (manager.whiteToMove ? -8 : 8)] = PieceUtil.TYPE_NONE;

            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.startSquare, move.targetSquare);
            manager.getOpponent().getPieces(PieceUtil.TYPE_PAWN).removePiece((byte)(move.targetSquare + (manager.whiteToMove ? -8 : 8)));
            manager.halfMoveClock = 0;
        } else {
            byte targetPiece = manager.board[move.targetSquare];
            byte startingPiece = PieceUtil.getPieceType(manager.board[move.startSquare]);

            if (targetPiece != PieceUtil.TYPE_NONE) {
                moveInfo.piece = targetPiece;
                zobrist.updateMoveHash(targetPiece, move.targetSquare, -1);
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

            zobrist.updateMoveHash(manager.board[move.startSquare], move.startSquare, move.targetSquare);
            manager.board[move.targetSquare] = manager.board[move.startSquare];
            manager.board[move.startSquare] = PieceUtil.TYPE_NONE;
        }

        zobrist.updatePlayerHash();
        if (manager.castleRights != 0) {
            zobrist.updateCastleHash(manager.castleRights);
        }
        manager.whiteToMove = !manager.whiteToMove;
        previousMoves.push(moveInfo);
    }

    private void unMakeMove(Move move) {
        zobrist.updatePlayerHash();
        if (manager.epSquare != -1) {
            zobrist.updateEPHash(manager.epSquare);
        }
        if (manager.castleRights != 0) {
            zobrist.updateCastleHash(manager.castleRights);
        }

        MoveInfo moveInfo = previousMoves.pop();
        manager.whiteToMove = !manager.whiteToMove;
        manager.epSquare = moveInfo.epSquare;
        manager.castleRights = moveInfo.castleRights;
        manager.halfMoveClock = moveInfo.halfMoveClock;

        if (manager.epSquare != -1) {
            zobrist.updateEPHash(manager.epSquare);
        }
        if (manager.castleRights != 0) {
            zobrist.updateCastleHash(manager.castleRights);
        }

        if (Move.isPromotionMove(move.moveType)) {
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).addPiece(move.startSquare);
            manager.getPlayer().getPieces(manager.board[move.targetSquare]).removePiece(move.targetSquare);
            zobrist.updateMoveHash(manager.board[move.targetSquare], move.targetSquare, -1);

            if (moveInfo.piece != PieceUtil.TYPE_NONE) {
                manager.getOpponent().getPieces(moveInfo.piece).addPiece(move.targetSquare);
                zobrist.updateMoveHash(moveInfo.piece, move.targetSquare, -1);
            }

            manager.board[move.startSquare] = (byte)(PieceUtil.TYPE_PAWN | manager.getPlayer().color);
            zobrist.updateMoveHash(manager.board[move.startSquare], move.startSquare, -1);
            manager.board[move.targetSquare] = moveInfo.piece;
        } else if (Move.MOVE_CKS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(kingSquare - 1), (byte)(kingSquare + 1));

            zobrist.updateMoveHash(manager.board[kingSquare - 1], kingSquare - 1, kingSquare + 1);
            manager.board[kingSquare + 1] = manager.board[kingSquare - 1];

            zobrist.updateMoveHash(manager.board[kingSquare], kingSquare, kingSquare - 2);
            manager.board[kingSquare - 2] = manager.board[kingSquare];

            manager.board[kingSquare] = manager.board[kingSquare - 1] = PieceUtil.TYPE_NONE;
            manager.getPlayer().kingSquare = move.startSquare;
        } else if (Move.MOVE_CQS == move.moveType) {
            byte kingSquare = manager.getPlayer().kingSquare;
            manager.getPlayer().getPieces(PieceUtil.TYPE_ROOK).updatePosition((byte)(kingSquare + 1), (byte)(kingSquare - 2));

            zobrist.updateMoveHash(manager.board[kingSquare + 1], kingSquare + 1, kingSquare - 2);
            manager.board[kingSquare - 2] = manager.board[kingSquare + 1];

            zobrist.updateMoveHash(manager.board[kingSquare], kingSquare, kingSquare + 2);
            manager.board[kingSquare + 2] = manager.board[kingSquare];

            manager.board[kingSquare] = manager.board[kingSquare + 1] = PieceUtil.TYPE_NONE;
            manager.getPlayer().kingSquare = move.startSquare;
        } else if (Move.MOVE_2_SQUARES == move.moveType) {
            zobrist.updateMoveHash(manager.board[move.targetSquare], move.targetSquare, move.startSquare);
            manager.board[move.startSquare] = manager.board[move.targetSquare];

            manager.board[move.targetSquare] = PieceUtil.TYPE_NONE;
            manager.getPlayer().getPieces(PieceUtil.TYPE_PAWN).updatePosition(move.targetSquare, move.startSquare);
        } else if (Move.MOVE_EP == move.moveType) {
            zobrist.updateMoveHash(manager.board[move.targetSquare], move.targetSquare, move.startSquare);
            manager.board[move.startSquare] = manager.board[move.targetSquare];
            manager.board[move.targetSquare] = PieceUtil.TYPE_NONE;

            zobrist.updateMoveHash(moveInfo.piece, move.targetSquare + (manager.whiteToMove ? -8 : 8), -1);
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
                zobrist.updateMoveHash(moveInfo.piece, move.targetSquare, -1);
                manager.getOpponent().getPieces(moveInfo.piece).addPiece(move.targetSquare);
            }

            zobrist.updateMoveHash(manager.board[move.targetSquare], move.targetSquare, move.startSquare);
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
        ArrayList<Move> legaMoves = generator.getLegalMoves(null, false);
        long moves = 0;
        for (Move move : legaMoves) {
            makeMove(move);
            long currentMoves = getNodeCount(depth - 1);
            moves += currentMoves;
            unMakeMove(move);
        }
        return moves;
    }

    private boolean updateSearchCancelled() {
        if (searchEndTime < System.currentTimeMillis()) {
            searchCancelled =  true;
        }
        return searchCancelled;
    }
}
