import game.Manager;
import util.BoardUtil;

public class BoardSetupTest {
    public static void main(String[] args) {
        Manager manger = new Manager();
        BoardUtil.displayBoard(manger.board);
    }
}
