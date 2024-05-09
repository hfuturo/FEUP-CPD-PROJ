import utils.Pair;

import java.io.*;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Authentication {

    private final Socket socket;

    public Authentication(Socket socket) {
        this.socket = socket;
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

    public boolean authenticate() {
        try {
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
                    scanner.nextLine();
                    continue;
                }

                if (selection != 0 && selection != 1) {
                    System.out.println("Input must be 0 or 1");
                    scanner.nextLine();
                    continue;
                }

                String operation = selection == 0 ? "login" : "register";
                String message = buildMessage(operation);

                System.out.println("sending authentication message to server: " + message);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(message);

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String response = reader.readLine(); // Read the server response once

                if(response.equals(operation + " successful")){
                    System.out.println("Authentication complete.");
                    return true;
                }

                System.out.println(response);
                System.out.println("Select an option:");
                System.out.println("[0] Login");
                System.out.println("[1] Register");
                System.out.println("Selection? ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
