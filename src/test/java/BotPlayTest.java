import game.v4.Bot;
import game.v4.Move;
import util.BoardUtil;

public class BotPlayTest {
    public static void main(String[] args) {
        Bot white = new Bot();
        Bot black = new Bot();
        boolean whiteToPlay = true;

        while (!white.hasGameEnded() && !black.hasGameEnded()) {
            Bot player = whiteToPlay ? white : black;
            Bot opponent = !whiteToPlay ? white : black;

            Move move = player.playBotMove();
            opponent.updatePosition(move);
            BoardUtil.displayBoard(player.getBoard());

            whiteToPlay = !whiteToPlay;
        }
    }
}
