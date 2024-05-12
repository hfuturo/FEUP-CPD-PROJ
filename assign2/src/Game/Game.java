package Game;


import Game.Words.Words;

import java.util.List;

public class Game {
    public static final int PLAYERS_REQUIRED = 1;
    private List<Player> players;
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
        for (Player player : players) {
            this.sendMessage(player, "Word: " + this.word);
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
