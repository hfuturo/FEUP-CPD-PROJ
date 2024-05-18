package game;

import utils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Player {

    private final String username;
    private Pair<PrintWriter, BufferedReader> serverComms;
    private double rank;
    private final List<String> wordsUsed;
    private boolean connected;
    private boolean playing;

    public Player(String username, Pair<PrintWriter, BufferedReader> serverComms, double rank) {
        this.username = username;
        this.serverComms = serverComms;
        this.rank = rank;
        this.wordsUsed = new ArrayList<>();
        this.connected = true;
        this.playing = false;
    }

    public PrintWriter getServerWriter() {
        return this.serverComms.getFirst();
    }

    public BufferedReader getServerReader() {
        return this.serverComms.getSecond();
    }

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

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public void setServerComms(Pair<PrintWriter, BufferedReader> serverComms) {
        this.serverComms = serverComms;
    }

    public boolean equals(Player player) {
        return this.getUsername().equals(player.getUsername());
    }
}
