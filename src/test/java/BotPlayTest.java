import game.Bot;
import game.Manager;
import game.Move;
import util.BoardUtil;

public class BotPlayTest {
    public static void main(String[] args) {
        Bot white = new Bot();
        Bot black = new Bot();
        boolean whiteToPlay = true;

        while (!white.hasGameEnded()) {
            Bot player = whiteToPlay ? white : black;
            Bot opponent = !whiteToPlay ? white : black;

            Move move = player.playBotMove();
            opponent.updatePosition(move);
            BoardUtil.displayBoard(player.getBoard());

            whiteToPlay = !whiteToPlay;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                System.out.println("Interrupted sleep...");
            }
        }
    }
}
