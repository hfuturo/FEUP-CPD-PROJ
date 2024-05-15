package game;

import game.words.Words;
import utils.Pair;
import utils.Protocol;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Game {
    public static final int PLAYERS_REQUIRED = 2;
    private final static double MAX_RANK_GAIN = 100;
    private final List<Player> players;
    private final ReentrantLock playersLock;
    private String word;
    private boolean stop;

    public Game(List<Player> players) {
        this.players = players;
        this.playersLock = new ReentrantLock();
        this.stop = false;

        Words words;
        try {
            words = new Words();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating file");
            return;
        }

        this.word = words.getRandomWord();
        System.out.println(this.word);
    }

    public void run() {

        List<Thread> threads = new ArrayList<>();

        // lança um thread por cada jogador
        for (Player player : players) {
            this.sendMessage(player, Protocol.INFO, "Game started!");

            Thread thread = Thread.ofVirtual().start(() -> {
                while (true) {

                    String word = this.getPlayerWord(player);

                    this.playersLock.lock();
                    boolean stop = this.stop;
                    this.playersLock.unlock();

                    if (stop) {
                        break;
                    }

                    if (this.word.equals(word)) {
                        this.handleWinner(player);
                        break;
                    }

                    String colored_word = this.colorWord(word);
                    player.addWordUsed(colored_word);
                    System.out.println("Word: " + this.word);

                    // mete linha nova no terminal para separar palavra escrita das palavras retornadas pelo server
                    this.sendMessage(player, Protocol.INFO, Protocol.EMPTY);
                    player.getWordsUsed().forEach((used_word) -> this.sendMessage(player, Protocol.INFO, used_word));
                }

                player.resetUsedWords();
                System.out.println(player.getUsername() + " left");

            });

            threads.add(thread);
        }

        // espera que threads acabem
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private void handleWinner(Player player) {
        this.playersLock.lock();

        List<Pair<String, Double>> newRankPlayers = new ArrayList<>();

        double newWinnerRank = this.updateRanks(player, player);

        newRankPlayers.add(new Pair<>(player.getUsername(), newWinnerRank));

        this.players.forEach(p ->  {
            this.sendMessage(p, Protocol.INFO, "Game ended!");
            this.sendMessage(p, Protocol.INFO, p.equals(player) ? "Yow won!" : "You lost");
            if (!p.equals(player)){
                double playerRank = this.updateRanks(player,p);
                newRankPlayers.add(new Pair<>(p.getUsername(),playerRank));
            }
            this.sendMessage(p, Protocol.INFO, "The word was '" + this.word + "'");
        });

        //Atualiza os novos ranks
        newRankPlayers.forEach(pair -> {
            String username = pair.getFirst();
            double rank = pair.getSecond();
            Player p = this.getPlayer(username);
            p.setRank(rank);
        });

        this.stop = true;
        this.playersLock.unlock();
    }

    private double updateRanks(Player winner, Player loser) {
        DecimalFormat df = new DecimalFormat("#.##");
        // update a si mesmo
        if (winner.equals(loser)) {
            List<Double> allProbabilities = new ArrayList<>();
            System.out.println("Entrou no winner igual ao loser");
            for (Player p : players) {
                if (!p.equals(winner)) {
                    //Probabilidade do winner perder
                    double probability = 1 - calculateRank(winner, p);
                    allProbabilities.add(probability);
                }
            }
                //Calcular a média das probabilidades do winner ganhar a cada um dos outros jogadores
                double sum = 0.0;
                for (double probability : allProbabilities) {
                    sum += probability;
                }

                double variation = sum / allProbabilities.size();
                double newRank = winner.getRank() + MAX_RANK_GAIN * variation;
                return Double.parseDouble(df.format(newRank));

        }
        else {
            // probabilidade de loser ganhar
            double variation = this.calculateRank(loser, winner);
            double newRank = loser.getRank() - MAX_RANK_GAIN * variation;
            return Double.parseDouble(df.format(newRank));
        }

    }

    // probabilidade de p1 ganhar
    private double calculateRank(Player p1, Player p2){
        double exponent = (p2.getRank() - p1.getRank()) / 400;
        return 1 / (1 + Math.pow(10, exponent));
    }

    private String getPlayerWord(Player player) {
        while (true) {
            // pede mensagem
            this.sendMessage(player, Protocol.REQUEST, "Type your word:");

            // recebe mensagem
            String word = this.receiveMessage(player);
            if (word == null) continue;
            word = word.trim().toLowerCase();

            System.out.println("received: " + word);

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

    private Player getPlayer(String username) {
        for (Player player : this.players) {
            if (player.getUsername().equals(username)) {
                return player;
            }
        }
        return null;
    }
}
