import utils.Protocol;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {

    private final String hostname;
    private final int port;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.socket = null;
    }

    private void listen() {
        try {

            while (true) {
                String[] command = this.reader.readLine().split("\\|");

                // flush input
                if (command.length == 1) continue;

                String type = command[0] + "|";
                String content = command[1];

                switch (type) {
                    case Protocol.PING -> this.writer.println(Protocol.ACK);
                    case Protocol.INFO -> System.out.println(content);
                    case Protocol.REQUEST -> {
                        System.out.println(content);
                        Scanner scanner = new Scanner(System.in);
                        String response = scanner.nextLine();
                        this.writer.println(response);
                    }
                    case Protocol.TERMINATE -> {
                        System.out.println(content);
                        this.reader.close();
                        this.writer.close();
                        return;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate() {
        Authentication authentication = new Authentication(this.writer, this.reader);
        return authentication.authenticate();
    }

    private void start() {
        try  {
            this.socket = new Socket(this.hostname, this.port);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            boolean ret = this.authenticate();

            if (ret) {
                this.listen();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error closing client socket");
                }
            }
        }
    }

    private static void usage() {
        System.out.println("Wrong usage");
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            Client.usage();
            return;
        }

        int port = Integer.parseInt(args[0]);
        String hostname = args.length == 1 ? "localhost" : args[1];

        try {
            Client client = new Client(hostname, port);
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}