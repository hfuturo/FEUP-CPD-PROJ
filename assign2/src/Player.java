import utils.Pair;

import java.io.*;
import java.net.Socket;

public class Player {

    private final String username;
    private final Socket socket;
    private final Pair<PrintWriter, BufferedReader> serverComms;
    private int rank;
    private int points;

    public Player(String username, Socket socket, Pair<PrintWriter, BufferedReader> serverComms) {
        this.username = username;
        this.socket = socket;
        this.serverComms = serverComms;
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

    public int getRank() {
        return this.rank;
    }

    public int getPoints() {
        return this.points;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
