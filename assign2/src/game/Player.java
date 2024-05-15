package game;

import utils.Pair;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Player {

    private final String username;
    private final Socket socket;
    private final Pair<PrintWriter, BufferedReader> serverComms;
    private double rank;
    private final List<String> wordsUsed;

    public Player(String username, Socket socket, Pair<PrintWriter, BufferedReader> serverComms, double rank) {
        this.username = username;
        this.socket = socket;
        this.serverComms = serverComms;
        this.rank = rank;
        this.wordsUsed = new ArrayList<>();
    }

    public PrintWriter getServerWriter() {
        return this.serverComms.getFirst();
    }

    public BufferedReader getServerReader() {
        return this.serverComms.getSecond();
    }

    public Socket getSocket() { return this.socket; }

    public String getUsername() {
        return this.username;
    }

    public double getRank() {
        return this.rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public void addWordUsed(String word) {
        this.wordsUsed.add(word);
    }

    public List<String> getWordsUsed() {
        return this.wordsUsed;
    }

    public void resetUsedWords() {
        this.wordsUsed.clear();
    }

    public boolean equals(Player player) {
        return this.getUsername().equals(player.getUsername());
    }
}
