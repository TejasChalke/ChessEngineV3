import game.v4.Bot;
import util.BoardUtil;

public class PositionTest {
    public static void main(String[] args) {
        runPositionTest("k7/8/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 1");
    }

    public static void runPositionTest(String fen) {
        Bot bot = new Bot(fen);
        BoardUtil.displayBoard(bot.getBoard());
        do {
            bot.playBotMove();
            BoardUtil.displayBoard(bot.getBoard());
        } while (!bot.hasGameEnded());
    }
}
