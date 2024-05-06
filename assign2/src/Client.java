import java.net.*;
import java.io.*;

public class Client {

    private final String hostname;
    private final int port;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    private void authenticate() {
        Authentication authentication = new Authentication(this.hostname, this.port);
        authentication.authenticate();
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
            client.authenticate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}