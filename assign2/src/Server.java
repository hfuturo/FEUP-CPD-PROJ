import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

    private static List<Player> waiting_players = new ArrayList<Player>();
    private static final Lock waiting_players_lock = new ReentrantLock();

    public static void handleClient(Socket socket) {
        try (PrintWriter server_writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader server_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String command = server_reader.readLine();

            System.out.println("Received command " + command);

            String[] tokens = command.split(";");
            String operation = tokens[0];
            String username = tokens[1];
            String password = tokens[2];

            Database database = new Database();
            boolean successful = operation.equals("login") ?
                    database.authenticateUser(username, password) :
                    database.registerUser(username, password);

            server_writer.println(successful ? "SUCCESS" : "ERROR");

            if (successful) {
                Player player = new Player(username, socket);
                synchronized (waiting_players_lock) {
                    waiting_players.add(player);
                    if (waiting_players.size() == 2) {
                        System.out.println("2 players found, starting game...");
                        char letter = (char) (Math.random() * 26 + 'a');
                        // Send GAME to both players
                        server_writer.println("GAME");

                        //TODO: Next start the game with both players
                        // Game game = new Game(waiting_players, letter);
                        // game.start();
                        waiting_players.clear();
                    }
                }
                System.out.println("Waiting players list: " + waiting_players.size() + " player(s)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) return;

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().start(() -> handleClient(socket));

               
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}