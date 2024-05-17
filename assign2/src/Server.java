import game.*;
import utils.Pair;
import utils.Protocol;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    // server
    private final int port;
    private static final int GAP_INCREMENT = 5;
    private static final int INITIAL_GAP = 10;
    private static final int GAP_INCREMENT_TIMER = 5;

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
    private final List<Player> clients;
    private final Lock clients_lock;

    public Server(int port) {
        // server
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
        this.clients = new ArrayList<>();
        this.clients_lock = new ReentrantLock();
    }

    private void addPlayerToNormalQueue(Player player) {
        this.sendMessage(player, Protocol.INFO, "Entered normal waiting queue");

        this.waiting_normal_lock.lock();
        this.waiting_normal.add(player);
        this.waiting_normal_lock.unlock();
    }

    private void addPlayerToRankedQueue(Player player) {
        this.sendMessage(player, Protocol.INFO, "Entered ranked waiting queue");

        this.waiting_ranked_lock.lock();
        this.waiting_ranked.add(player);
        this.waiting_ranked_lock.unlock();
    }

    private void addPlayerToQueue(Player player, int mode) {
        if (mode == Game.Modes.NORMAL.ordinal()) {
            this.addPlayerToNormalQueue(player);
        }
        else {
            this.addPlayerToRankedQueue(player);
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
        this.clients.remove(player);
        this.clients_lock.unlock();

        System.out.println(player.getUsername() + " left the game");
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

        player.setPlaying(false);

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

    private void startRankedGame(Player player, int time_elapsed) {
        if (!player.isConnected()) return;

        this.waiting_ranked_lock.lock();
        System.out.println(player.getUsername() + " gap: " +
                (Server.INITIAL_GAP + (Math.floor((double)time_elapsed / Server.GAP_INCREMENT_TIMER)) * Server.GAP_INCREMENT) +
                " time_elapsed: " + time_elapsed);

        if (this.waiting_ranked.size() >= Game.PLAYERS_REQUIRED) {
            List<Player> players = new ArrayList<>();
            players.add(player);

            for (Player p : this.waiting_ranked) {
                if (!player.equals(p) && p.isConnected() &&
                        Math.abs(player.getRank() - p.getRank()) <= Server.INITIAL_GAP + (Math.floor((double)time_elapsed / Server.GAP_INCREMENT_TIMER)) * Server.GAP_INCREMENT)
                {
                    players.add(p);
                }

                if (players.size() == Game.PLAYERS_REQUIRED) {
                    for (int i = 0; i < Game.PLAYERS_REQUIRED; i++) {
                        this.waiting_ranked.remove(players.get(i));
                    }
                    break;
                }
            }

            this.waiting_ranked_lock.unlock();

            if (players.size() != Game.PLAYERS_REQUIRED) {
                return;
            }


            players.forEach(p -> {
                p.setPlaying(true);
                this.sendMessage(p, Protocol.INFO, "Game is about to start!");
            });

            Game game = new Game(players, Game.Modes.RANKED.ordinal());
            game.run();

            this.finished_ranked_lock.lock();
            this.finished_ranked.addAll(players);
            this.finished_ranked_lock.unlock();
        }
        else {
            this.waiting_ranked_lock.unlock();
        }
    }

    private void startNormalGame() {
        this.waiting_normal_lock.lock();

        if (this.waiting_normal.size() >= Game.PLAYERS_REQUIRED) {
            List<Player> players = new ArrayList<>();

            for (Player player : this.waiting_normal) {
                if (!player.isConnected()) continue;

                players.add(player);

                if (players.size() == Game.PLAYERS_REQUIRED) {
                    for (int i = 0; i < Game.PLAYERS_REQUIRED; i++) {
                        this.waiting_normal.remove(players.get(i));
                    }
                    break;
                }
            }

            this.waiting_normal_lock.unlock();

            if (players.size() != Game.PLAYERS_REQUIRED) {
                return;
            }

            players.forEach(player -> {
                player.setPlaying(true);
                this.sendMessage(player, Protocol.INFO, "Game is about to start!");
            });

            Game game = new Game(players, Game.Modes.NORMAL.ordinal());
            game.run();

            this.finished_normal_lock.lock();
            this.finished_normal.addAll(players);
            this.finished_normal_lock.unlock();
        }
        else {
            this.waiting_normal_lock.unlock();
        }
    }



    private void startGame(Player player, int mode, int time_elapsed) {
        if (!player.isConnected()) return;

        if (mode == Game.Modes.NORMAL.ordinal()) {
            this.startNormalGame();
        } else {
            this.startRankedGame(player, time_elapsed);
        }
    }

    private Pair<Player, Boolean> authenticateClient(Socket socket, PrintWriter server_writer, BufferedReader server_reader) {
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
                if (successful) database.assignToken(username);
                this.database_lock.unlock();

                if (successful) {
                    this.clients_lock.lock();

                    Pair<PrintWriter, BufferedReader> commsChannels = new Pair<>(server_writer, server_reader);
                    Player player;
                    boolean userReconnected;

                    // se tiver conectado, significa que perdeu conexao ou outra pessoa tenta entrar na conta
                    if (this.clientConnected(username)) {
                        player = this.getPlayerByUsername(username);

                        if (player == null) {
                            successful = false;
                            this.clients_lock.unlock();
                            continue;
                        }

                        // outra pessoa tenta entrar na conta
                        if (player.isConnected()) {
                            successful = false;
                            server_writer.println(operation + " error (user already logged in)");
                            this.clients_lock.unlock();
                            continue;
                        }

                        System.out.println(username + " reconnected");
                        player.setServerComms(commsChannels);
                        userReconnected = true;
                        server_writer.println(operation + " successful");
                    }
                    else {
                        System.out.println(username + " connected");
                        player = new Player(username, socket, commsChannels, rank);
                        clients.add(player);
                        userReconnected = false;
                        server_writer.println(operation + " successful");
                    }

                    this.clients_lock.unlock();
                    return new Pair<>(player, userReconnected);

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

    private boolean clientConnected(String username) {
        this.clients_lock.lock();

        for (Player p : this.clients) {
            if (p.getUsername().equals(username)) {
                this.clients_lock.unlock();
                return true;
            }
        }

        this.clients_lock.unlock();
        return false;
    }

    private Player getPlayerByUsername(String username) {
        for (Player player : this.clients) {
            if (player.getUsername().equals(username))
                return player;
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
        int time_elapsed = 0;

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
                this.startGame(player, mode, time_elapsed);
            }
            else {
                this.handleFinishedPlayers(player, mode);
            }

            // espera 1sec e tenta novamente
            try {
                Thread.sleep(1000);

                if (player.isConnected()) {
                    time_elapsed++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pingClients() {
        while (true) {

            this.clients_lock.lock();

            for (Player player : this.clients) {
                if (player.isPlaying()) continue;

                Thread.ofVirtual().start(() -> {
                    this.sendMessage(player, Protocol.PING, Protocol.EMPTY);
                    String message = this.receiveMessage(player);
                    player.setConnected(message != null);
                });
            }

            this.clients_lock.unlock();

            // espera 1sec e pinga novamente
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
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {

            System.out.println("Server is listening on port " + this.port);

            Thread.ofVirtual().start(this::pingClients);

            while (true) {
                Socket socket = serverSocket.accept();
                PrintWriter server_writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader server_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Thread.ofVirtual().start(() -> {
                    Pair<Player, Boolean> info = this.authenticateClient(socket, server_writer, server_reader);

                    Player player = info.getFirst();
                    boolean reconnected = info.getSecond();

                    int mode;

                    if (reconnected) {
                        mode = this.getModeByPlayer(player);
                        player.setConnected(true);
                    }
                    else {
                        mode = this.getClientGameMode(player);
                        this.addPlayerToQueue(player, mode);
                        this.handlePlayer(player, mode);
                    }

                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getModeByPlayer(Player player) {
        this.waiting_ranked_lock.lock();
        this.finished_ranked_lock.lock();
        boolean isRanked = waiting_ranked.contains(player) || finished_ranked.contains(player);
        this.waiting_ranked_lock.unlock();
        this.finished_ranked_lock.unlock();

        return isRanked ?
                Game.Modes.RANKED.ordinal() :
                Game.Modes.NORMAL.ordinal();
    }

    public static void main(String[] args) {
        if (args.length != 1) return;

        int port = Integer.parseInt(args[0]);

        try {
            Server server = new Server(port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}