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
    // server
    private final String hostname;
    private final int port;
    private ServerSocket serverSocket;

    // queues
    private final List<Player> waiting_normal;
    private final Lock waiting_normal_lock;
    private final List<Player> waiting_ranked;
    private final Lock waiting_ranked_lock;

    // finished queue
    private final List<Player> finished_normal;
    private final Lock finished_normal_lock;
    private final List<Player> finished_ranked;
    private final Lock finished_ranked_lock;

    // db
    private final Database database;
    private final Lock database_lock;
    private final HashMap<String, Pair<PrintWriter, BufferedReader>> clients;
    private final Lock clients_lock;

    public Server(String hostname, int port) {
        // server
        this.hostname = hostname;
        this.port = port;

        // waiting queue
        this.waiting_normal = new ArrayList<>();
        this.waiting_normal_lock = new ReentrantLock();
        this.waiting_ranked = new ArrayList<>();
        this.waiting_ranked_lock = new ReentrantLock();

        // finished queue
        this.finished_normal = new ArrayList<>();
        this.finished_normal_lock = new ReentrantLock();
        this.finished_ranked = new ArrayList<>();
        this.finished_ranked_lock = new ReentrantLock();

        // db
        this.database_lock = new ReentrantLock();
        this.database = new Database();
        this.clients = new HashMap<>();
        this.clients_lock = new ReentrantLock();
    }

    private void addPlayerToQueue(Player player, int mode) {
        this.sendMessage(player, Protocol.INFO, "Entered " + (mode == 0 ? "normal" : "ranked") + " waiting queue");

        if (mode == Game.Modes.NORMAL.ordinal()) {
            this.waiting_normal_lock.lock();
            this.waiting_normal.add(player);
            System.out.println(player.getUsername() + " entered waiting queue for simple mode.");
            this.waiting_normal_lock.unlock();
        }
        else {
            this.waiting_ranked_lock.lock();
            this.waiting_ranked.add(player);
            System.out.println(player.getUsername() + " entered waiting queue for rank mode.");
            this.waiting_ranked_lock.unlock();
        }
    }


    private void disconnectClient(Player player) {
        this.sendMessage(player, Protocol.TERMINATE, "Terminating connection.");

        this.finished_normal_lock.lock();
        this.finished_normal.remove(player);
        this.finished_normal_lock.unlock();

        this.finished_ranked_lock.lock();
        this.finished_ranked.remove(player);
        this.finished_ranked_lock.unlock();

        this.clients_lock.lock();
        this.clients.remove(player.getUsername());
        this.clients_lock.unlock();
    }

    private void handleFinishedPlayers(Player player, int mode) {
        boolean player_finished_playing;
        if (mode == Game.Modes.NORMAL.ordinal()) {
            this.finished_normal_lock.lock();
            player_finished_playing = this.finished_normal.contains(player);
            this.finished_normal_lock.unlock();
        }
        else {
            this.finished_ranked_lock.lock();
            player_finished_playing = this.finished_ranked.contains(player);
            this.finished_ranked_lock.unlock();
        }

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


        if (mode == Game.Modes.NORMAL.ordinal()) {
            this.finished_normal_lock.lock();
            this.finished_normal.remove(player);
            this.finished_normal_lock.unlock();
        }
        else {
            this.finished_ranked_lock.lock();
            this.finished_ranked.remove(player);
            this.finished_ranked_lock.unlock();
        }

        if (response.equals("y")) {
            this.addPlayerToQueue(player, mode);
        }
        else {
            this.database.updateRankDatabase(player.getRank(), player.getUsername());
            this.disconnectClient(player);
        }

    }

    private void startGame(int mode) {

        if (mode == Game.Modes.NORMAL.ordinal()) {
            this.waiting_normal_lock.lock();
        } else {
            this.waiting_ranked_lock.lock();
        }

        int size = mode == Game.Modes.NORMAL.ordinal() ? this.waiting_normal.size() : this.waiting_ranked.size();
        if (size >= Game.PLAYERS_REQUIRED) {
            List<Player> players = new ArrayList<>();

            for (int i = 0; i < Game.PLAYERS_REQUIRED; i++) {
                Player player = mode == Game.Modes.NORMAL.ordinal() ? this.waiting_normal.remove(0) : this.waiting_ranked.remove(0);

                players.add(player);
            }

            if (mode == Game.Modes.NORMAL.ordinal()) {
                this.waiting_normal_lock.unlock();
            } else {
                this.waiting_ranked_lock.unlock();
            }

            players.forEach(player -> this.sendMessage(player, Protocol.INFO, "Game is about to start!"));
            Game game = new Game(players, mode);
            game.run();

            if (mode == Game.Modes.NORMAL.ordinal()) {
                this.finished_normal_lock.lock();
                this.finished_normal.addAll(players);
                this.finished_normal_lock.unlock();
            } else {
                this.finished_ranked_lock.lock();
                this.finished_ranked.addAll(players);
                this.finished_ranked_lock.unlock();
            }
        }
        else {
            if (mode == Game.Modes.NORMAL.ordinal()) {
                this.waiting_normal_lock.unlock();
            } else {
                this.waiting_ranked_lock.unlock();
            }
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
        player.getServerWriter().println(protocol + message + "\n");
    }

    private String receiveMessage(Player player) {
        try {
            return player.getServerReader().readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private void handlePlayer(Player player, int mode) {
        while (true) {
            boolean player_in_queue;

            if (mode == Game.Modes.NORMAL.ordinal()) {
                this.waiting_normal_lock.lock();
                player_in_queue = this.waiting_normal.contains(player);
                this.waiting_normal_lock.unlock();
            }
            else {
                this.waiting_ranked_lock.lock();
                player_in_queue = this.waiting_ranked.contains(player);
                this.waiting_ranked_lock.unlock();
            }

            // se player na queue, tenta começar jogo
            if (player_in_queue) {
                this.startGame(mode);
            }
            else {
                this.handleFinishedPlayers(player, mode);
            }

            // espera 1sec e tenta novamente
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getClientGameMode(Player player) {
        this.sendMessage(player, Protocol.INFO, "Which mode do tou want to play?");
        this.sendMessage(player, Protocol.INFO, "[0] Simple mode");
        this.sendMessage(player, Protocol.REQUEST, "[1] Ranked mode");

        while (true) {
            String input = this.receiveMessage(player);
            if (input == null) continue;

            input = input.trim().toLowerCase();
            if (!input.equals("0") && !input.equals("1")) {
                this.sendMessage(player, Protocol.REQUEST, "Input must be 0 or 1");
                continue;
            }

            return Integer.parseInt(input);
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
                    int mode = this.getClientGameMode(player);
                    this.addPlayerToQueue(player, mode);
                    this.handlePlayer(player, mode);
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