import java.io.IOException;
import java.util.List;

public class Game {
    private List<Player> players;
    private char letter;

    Game(List<Player> players, char letter) {
        this.players = players;
        this.letter = letter;

    }

    public void start() throws IOException {

        System.out.println("The chosen letter is: " + letter);

    }
}
