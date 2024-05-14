package Game;

import Game.Words.Words;
import utils.Protocol;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Game {
    public static final int PLAYERS_REQUIRED = 2;
    private final List<Player> players;
    private final ReentrantLock playersLock;
    private Words words;
    private String word;
    private boolean stop;

    public Game(List<Player> players) {
        this.players = players;
        this.playersLock = new ReentrantLock();

        try {
            this.words = new Words();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating file");
            return;
        }

        this.stop = false;
        this.word = this.words.getRandomWord();
        System.out.println(this.word);
    }

    public void run() {

        // lança um thread por cada jogador
        for (Player player : players) {
            this.sendMessage(player, Protocol.INFO, "Game started!");

            Thread.ofVirtual().start(() -> {
                while (true) {

                    String word = this.getPlayerWord(player);

                    this.playersLock.lock();
                    if (this.stop) {
                        break;
                    }
                    this.playersLock.unlock();

                    if (this.word.equals(word)) {
                        this.playersLock.lock();
                        this.players.forEach(p ->  {
                            this.sendMessage(p, Protocol.INFO, "Game ended!");
                            this.sendMessage(p, Protocol.INFO, p.equals(player) ? "Yow won!" : "You lost");
                            this.sendMessage(p, Protocol.INFO, "The word was " + this.word);
                        });
                        this.stop = true;
                        this.playersLock.unlock();
                        break;
                    }

                    String colored_word = this.colorWord(word);
                    player.addWordUsed(colored_word);
                    System.out.println("Word: " + this.word);

                    // mete linha nova no terminal para separar palavra escrita das palavras retornadas pelo server
                    this.sendMessage(player, Protocol.INFO, Protocol.EMPTY);
                    player.getWordsUsed().forEach((used_word) -> this.sendMessage(player, Protocol.INFO, used_word));
                }

                System.out.println(player.getUsername() + " left");
            });
        }
    }

    private String getPlayerWord(Player player) {
        while (true) {
            // pede mensagem
            this.sendMessage(player, Protocol.REQUEST, "Type your word:");

            // recebe mensagem
            String word = this.receiveMessage(player);
            if (word == null) continue;
            word = word.trim().toLowerCase();

            System.out.println("received: " + word.toString());

            if (word.length() == 5)
                return word;

            this.sendMessage(player, Protocol.INFO, "Word must have 5 letters.");
        }
    }

    private void sendMessage(Player player, String protocol, String message) {
        player.getServerWriter().println(protocol + message);
    }

    private String receiveMessage(Player player) {
        try {
            return player.getServerReader().readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private String colorWord(String word) {
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
