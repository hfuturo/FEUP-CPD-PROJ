package Game;

import Game.Words.Words;

import java.util.List;

public class Game {
    public static final String FINISH_GAME = "FINISH_GAME";
    public static final int PLAYERS_REQUIRED = 2;
    private final List<Player> players;
    private Words words;
    private String word;

    public Game(List<Player> players) {
        this.players = players;

        try {
            this.words = new Words();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating file");
            return;
        }

        this.word = this.words.getRandomWord();
        System.out.println(this.word);
    }

    public void run() {

        // lança um thread por cada jogador
        for (Player player : players) {
            this.sendMessage(player, "GAME STARTED!");

            Thread.ofVirtual().start(() -> {
                while (true) {


                    String word = this.receiveMessage(player);
                    System.out.println("received: " + word.toString());

                    if (this.word.equals(word)) {
                        this.players.forEach(p -> this.sendMessage(p, Game.FINISH_GAME));
                        break;
                    }

                    String verified_word = this.verifyWord(word);
                    player.addWordUsed(verified_word);
                    System.out.println("Word: " + this.word);
                    StringBuilder builder = new StringBuilder();
                    player.getWordsUsed().forEach((used_word) -> {
                        builder.append(used_word).append(",");
                    });

                    this.sendMessage(player, builder.deleteCharAt(builder.length() - 1).toString());
                }

            });
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

    private String verifyWord(String word) {
        StringBuilder string = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == this.word.charAt(i)) {
                string.append(Words.GREEN);
            }
            else if (this.word.contains(String.valueOf(word.charAt(i)))) {
                string.append(Words.YELLOW);
            }
            else {
                string.append(Words.DEFAULT_COLOR);
            }
            string.append(word.charAt(i));
        }

        // makes sure we go back to default color
        string.append(Words.DEFAULT_COLOR);

        return string.toString();
    }
}
