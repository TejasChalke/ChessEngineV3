import game.Manager;
import util.BoardUtil;

public class BoardSetupTest {
    public static void main(String[] args) {
        Manager manager = new Manager("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
//        Manager manager = new Manager();
        BoardUtil.displayBoard(manager.board);
        manager.test(3);
        manager.test(4);
    }
}
