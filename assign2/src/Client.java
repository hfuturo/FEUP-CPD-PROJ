import java.net.*;
import java.io.*;

public class Client {

    private final String hostname;
    private final int port;
    private Socket socket;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.socket = null;
    }

    private void listen() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            while (true) {
                String response = reader.readLine();
                System.out.println("received from server: " + response);
                writer.println("Okay chief!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate() {
        Authentication authentication = new Authentication(this.socket);
        return authentication.authenticate();
    }

    private void start() {
        try  {
            this.socket = new Socket(this.hostname, this.port);
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