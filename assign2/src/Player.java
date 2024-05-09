import java.io.*;
import java.net.Socket;

public class Player {

    private final String username;
    private final Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private int rank;
    private int points;

    public Player(String username, Socket socket) {
        this.username = username;
        this.socket = socket;

        try {
            this.writer = new PrintWriter(this.socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            System.out.println(this.username + " connected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PrintWriter getWriter() {
        return this.writer;
    }

    public BufferedReader getReader() {
        return this.reader;
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
