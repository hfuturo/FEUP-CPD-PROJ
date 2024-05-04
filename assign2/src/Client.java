import java.net.*;
import java.io.*;

public class Client {

    public static void main(String[] args) {
        if (args.length < 1) return;

        //String hostname = args[0];
        int port = Integer.parseInt(args[0]);

        try (Socket socket = new Socket("localhost", port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println("new Date()?".toString());

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String time = reader.readLine();

            System.out.println(time);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}