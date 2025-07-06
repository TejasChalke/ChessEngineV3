import game.Manager;
import util.BoardUtil;

public class BoardSetupTest {
    public static void main(String[] args) {
        Manager manger = new Manager("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        BoardUtil.displayBoard(manger.board);
        manger.test(3);
    }
}
