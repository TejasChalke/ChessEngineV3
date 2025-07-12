import game.v3.Bot;
import util.BoardUtil;

public class PositionTest {
    public static void main(String[] args) {
        runPositionTest("8/3K4/4P3/8/8/8/6k1/7q w - - 0 1");
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
