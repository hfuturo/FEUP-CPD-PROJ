import game.*;
import utils.Pair;
import utils.Protocol;

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
    private final List<Player> players_finished_game;
    private final Lock players_finished_lock;
    private final Database database;
    private final Lock database_lock;
    private ServerSocket serverSocket;
    private final HashMap<String, Pair<PrintWriter, BufferedReader>> clients;
    private final Lock clients_lock;

    public Server(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.waiting_players = new ArrayList<>();
        this.waiting_players_lock = new ReentrantLock();
        this.players_finished_game = new ArrayList<>();
        this.players_finished_lock = new ReentrantLock();
        this.database_lock = new ReentrantLock();
        this.database = new Database();
        this.clients = new HashMap<>();
        this.clients_lock = new ReentrantLock();
    }

    private void addPlayerToQueue(Player player) {
        this.sendMessage(player, Protocol.INFO, "Entered waiting queue");
        this.waiting_players_lock.lock();
        this.waiting_players.add(player);
        System.out.println(player.getUsername() + " entered waiting queue.");
        this.waiting_players_lock.unlock();
    }

    private void disconnectClient(Player player) {
        this.sendMessage(player, Protocol.TERMINATE, "Terminating connection.");

        this.waiting_players_lock.lock();
        this.waiting_players.remove(player);
        this.waiting_players_lock.unlock();

        this.players_finished_lock.lock();
        this.players_finished_game.remove(player);
        this.players_finished_lock.unlock();

        this.clients_lock.lock();
        this.clients.remove(player.getUsername());
        this.clients_lock.unlock();
    }

    private void handleFinishedPlayers(Player player) {
        this.players_finished_lock.lock();
        boolean player_finished_playing = this.players_finished_game.contains(player);
        this.players_finished_lock.unlock();

        if (!player_finished_playing)
            return;

        this.sendMessage(player, Protocol.REQUEST, "Do you want to play again? (y/n)");
        String response;
        while (true) {
            response = this.receiveMessage(player);

            if (response == null) continue;

            response = response.trim().toLowerCase();

            if (!response.equals("y") && !response.equals("n")) {
                this.sendMessage(player, Protocol.REQUEST, "Input must be 'y' or 'n'");
                continue;
            }

            break;
        }


        this.players_finished_lock.lock();
        this.players_finished_game.remove(player);
        this.players_finished_lock.unlock();

        if (response.equals("y")) {
            this.addPlayerToQueue(player);
        }
        else {
            this.database.updateRankDatabase(player.getRank(), player.getUsername());
            this.disconnectClient(player);
        }

    }

    private void startGame() {
        this.waiting_players_lock.lock();

        if (this.waiting_players.size() >= Game.PLAYERS_REQUIRED) {
            List<Player> players = new ArrayList<>();

            for (int i = 0; i < Game.PLAYERS_REQUIRED; i++) {
                Player player = this.waiting_players.remove(0);

                players.add(player);
            }

            this.waiting_players_lock.unlock();

            players.forEach(player -> {
                this.sendMessage(player, Protocol.INFO, "Game is about to start!");
                this.sendMessage(player, Protocol.INFO, "RANK " + player.getRank());

            });
            Game game = new Game(players);
            game.run();

            this.players_finished_lock.lock();
            this.players_finished_game.addAll(players);
            this.players_finished_lock.unlock();
        }
        else {
            this.waiting_players_lock.unlock();
        }
    }

    private Player authenticateClient(Socket socket, PrintWriter server_writer, BufferedReader server_reader) {
        try {
            boolean successful = false;
            while (!successful) {
                String command = server_reader.readLine();
                System.out.println("Received command: " + command);

                String[] tokens = command.split(";");
                String operation = tokens[0];
                String username = tokens[1];
                String password = tokens[2];

                this.database_lock.lock();
                successful = operation.equals("login") ?
                        this.database.authenticateUser(username, password) :
                        this.database.registerUser(username, password);
                double rank = successful ? this.database.getRankFromUser(username) : -1;
                this.database_lock.unlock();

                if (successful) {
                    if (clients.containsKey(username)) {
                        successful = false;
                        server_writer.println(operation + " error (User is already logged in)");
                    }
                    else {
                        System.out.println(username + " connected");
                        Pair<PrintWriter, BufferedReader> commsChannels = new Pair<>(server_writer, server_reader);
                        clients.put(username, commsChannels);
                        server_writer.println(operation + " successful");
                        return new Player(username, socket, commsChannels, rank);
                    }
                }
                else {
                    server_writer.println(operation + " error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return null;
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

    private void handlePlayer(Player player) {
        while (true) {
            this.waiting_players_lock.lock();
            boolean player_in_queue = this.waiting_players.contains(player);
            this.waiting_players_lock.unlock();

            // se player na queue, tenta começar jogo
            if (player_in_queue) {
                this.startGame();
            }
            else {
                this.handleFinishedPlayers(player);
            }

            // espera 1sec e tenta novamente
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                    this.addPlayerToQueue(player);
                    this.handlePlayer(player);
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