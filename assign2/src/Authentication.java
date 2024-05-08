import utils.Pair;

import java.io.*;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Authentication {

    private final String hostname;
    private final int port;

    public Authentication(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    private String buildMessage(String operation) {
        System.out.println("\n\n" + operation.toUpperCase());
        Pair<String, String> data = getInputs();
        String username = data.getFirst();
        String password = data.getSecond();

        return operation + ";" + username + ";" + password;
    }

    private Pair<String, String> getInputs() {
        Scanner scanner = new Scanner(System.in);
        String username, password;

        while (true) {
            System.out.print("Username: ");
            username = scanner.nextLine();

            if (this.validInput(username)) break;
            System.out.println("Username must not be empty.\n");
        }

        while (true) {
            System.out.print("Password: ");
            password = scanner.nextLine();

            if (this.validInput(password)) break;
            System.out.println("Password must not be empty.\n");
        }

        return new Pair<>(username, password);
    }

    private boolean validInput(String input) {
        return input.trim().length() != 0;
    }

    public void authenticate() {
        try (Socket socket = new Socket(this.hostname, this.port)) {
            System.out.println("Select an option:");
            System.out.println("[0] Login");
            System.out.println("[1] Register");
            System.out.println("Selection? ");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                int selection;

                try {
                    selection = scanner.nextInt();
                } catch (InputMismatchException e) {
                    System.out.println("Input must be an integer");
                    continue;
                }

                if (selection != 0 && selection != 1) {
                    System.out.println("Input must be 0 or 1");
                    continue;
                }

                String message = selection == 0 ?
                        buildMessage("login") :
                        buildMessage("register");

                System.out.println("sending authentication message to server: " + message);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(message);

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String response = reader.readLine(); // Read the server response once

                System.out.println("received from server: " + response);

                if(response.equals("SUCCESS")){
                    System.out.println("Authentication complete.");
                    System.out.println("Waiting for players to join...");
                    break;
                }
            }
            while (true){
                
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String response = reader.readLine();


                if (response != null && response.equals("GAME")) {
                    System.out.println("|| 2 PLAYERS FOUND ||");
                    System.out.println("Here we go! Game is starting...");
                    break;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
