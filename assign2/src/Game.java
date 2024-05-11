import java.io.IOException;
import java.util.List;

public class Game {
    public static final int PLAYERS_REQUIRED = 2;
    private List<Player> players;
    private char letter;

    public Game(List<Player> players) {
        this.players = players;
        this.letter = (char) (Math.random() * 26 + 'a');
    }

    public void run() {
        for (Player player : players) {
            this.sendMessage(player, "Letter: " + this.letter);
            System.out.println(this.receiveMessage(player));
        }
    }

    private void sendMessage(Player player, String message) {
        player.getServerWriter().println(message);
    }

    private String receiveMessage(Player player) {
        try {
            return player.getServerReader().readLine();
        } catch (Exception e) {
            return null;
        }
    }
}
