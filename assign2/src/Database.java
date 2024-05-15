import utils.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.Scanner;

public class Database {

    private static final String FILE_PATH = "database/database.txt";
    private final File file;

    public Database() {
        this.file = new File(Database.FILE_PATH);

        this.createFile();
    }

    private void createFile() {
         this.file.getParentFile().mkdir(); // cria dirs se não existirem

        try {
            boolean fileExists = !this.file.createNewFile();  // cria file se nao existir
            if (fileExists) return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(Database.FILE_PATH, true))) {
            fileWriter.write("henrique," + this.hash("henrique123") + ",150\n");
            fileWriter.write("joao," + this.hash("joao123") + ",170\n");
            fileWriter.write("tiago," + this.hash("tiago123") + ",200\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean authenticateUser(String username, String password) {
        try (Scanner scanner = new Scanner(this.file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(",");
                String passwordHash = this.hash(password);

                if (tokens[0].equals(username) && tokens[1].equals(passwordHash))
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean registerUser(String username, String password) {
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(Database.FILE_PATH, true));
                Scanner scanner = new Scanner(this.file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(",");

                if (tokens[0].equals(username))
                    return false;
            }

            fileWriter.write(username + "," + this.hash(password) + ",0\n");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    public double getRankFromUser (String username){
        try (Scanner scanner = new Scanner(this.file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(",");


                if (tokens[0].equals(username))
                    return Double.parseDouble(tokens[2]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
