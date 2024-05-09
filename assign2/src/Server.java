import utils.Pair;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

    private final String hostname;
    private final int port;
    private final List<Player> waiting_players;
    private final Lock waiting_players_lock;
    private final Database database;
    private final Lock database_lock;
    private ServerSocket serverSocket;
    private final HashMap<String, Pair<PrintWriter, BufferedReader>> clients;

    public Server(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.waiting_players = new ArrayList<>();
        this.waiting_players_lock = new ReentrantLock();
        this.database_lock = new ReentrantLock();
        this.database = new Database();
        this.clients = new HashMap<>();
    }

    private void addPlayerToQueue(Player player, PrintWriter writer, BufferedReader reader) {
        this.waiting_players_lock.lock();

        this.waiting_players.add(player);
        System.out.println(player.getUsername() + " entered waiting queue.");
        this.waiting_players_lock.unlock();

        if (this.waiting_players.size() == 2) {
            this.waiting_players.forEach((p) -> writer.println(p.getUsername()));
        }
        else {
            System.out.println("Waiting for game to start...");
            while (this.waiting_players.size() != 2) {}
            System.out.println("SAI");
        }
    }

    private Player authenticateClient(Socket socket, PrintWriter writer, BufferedReader reader) {
        try {

            boolean successful = false;
            while (!successful) {
                String command = reader.readLine();
                System.out.println("Received command: " + command);

                String[] tokens = command.split(";");
                String operation = tokens[0];
                String username = tokens[1];
                String password = tokens[2];

                this.database_lock.lock();
                successful = operation.equals("login") ?
                        this.database.authenticateUser(username, password) :
                        this.database.registerUser(username, password);
                this.database_lock.unlock();


                if (successful) {
                    if (clients.containsKey(username)) {
                        successful = false;
                        writer.println(operation + " error (User is already logged in)");
                    }
                    else {
                        clients.put(username, new Pair<>(writer, reader));
                        writer.println(operation + " successful");
                        return new Player(username, socket);
                    }
                }
                else {
                    writer.println(operation + " error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    private void start() {
        try {
            this.serverSocket = new ServerSocket(this.port);

            System.out.println("Server is listening on port " + this.port);

            while (true) {
                Socket socket = serverSocket.accept();
                PrintWriter server_writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader server_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Thread.ofVirtual().start(() -> {
                    Player player = this.authenticateClient(socket, server_writer, server_reader);
                    this.addPlayerToQueue(player, server_writer, server_reader);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) return;

        int port = Integer.parseInt(args[0]);
        String hostname = args.length == 1 ? "localhost" : args[1];

        try {
            Server server = new Server(hostname, port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}