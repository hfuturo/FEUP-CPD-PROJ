import java.io.*;
import java.net.*;
import java.util.Date;

/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class Server {

    public static void main(String[] args) {
        if (args.length < 1) return;

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String command = reader.readLine();

                System.out.println("Received command " + command);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                String[] tokens = command.split(";");
                String operation = tokens[0];
                String username = tokens[1];
                String password = tokens[2];

                Database database = new Database();
                boolean sucessfull = operation.equals("login") ?
                        database.authenticateUser(username, password) :
                        database.registerUser(username, password);

                writer.println(sucessfull ? "SUCCESS" : "ERROR");

                //System.out.println("entra");
                //while (true) {}
                //System.out.println("sai");
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}