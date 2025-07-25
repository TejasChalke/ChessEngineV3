import game.v4.Manager;

public class BoardSetupTest {
    public static void main(String[] args) {
//        runTestMoveGen("k7/8/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 1", 6, 119060324L);
        runTestMoveGen("", 6, 119060324L);
        runTestMoveGen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -", 5, 193690690);
        runTestMoveGen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 6, 11030083);
        runTestMoveGen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 6, 706045033);
        runTestMoveGen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 5, 89941194L);
        runTestMoveGen("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10", 5, 164075551);
    }

    public static void runTestMoveGen(String fen, int depth, long expectedCount) {
        Manager manager = fen != null && !fen.isEmpty() ? new Manager(fen) : new Manager();
        long result = manager.test(depth);
        if (expectedCount == result) {
            System.out.println("Test passed for : [" + fen + "] at depth (" + depth + ") with node count : " + result);
        } else {
            System.err.println("Test failed for : [" + fen + "] at depth (" + depth + ") with node count : " + result);
        }
    }
}
