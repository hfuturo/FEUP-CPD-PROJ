import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public void updateRankDatabase(double rank, String username) {
        try {
            File tempFile = new File(Database.FILE_PATH + ".temp");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
            Scanner scanner = new Scanner(this.file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(",");

                if (tokens[0].equals(username)) {
                    tokens[2] = String.valueOf(rank);
                    line = String.join(",", tokens);
                }

                writer.write(line + "\n");
            }

            writer.close();
            scanner.close();

            this.file.delete();
            tempFile.renameTo(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void assignToken(String username) {
        try {
            File tempFile = new File(Database.FILE_PATH + ".temp");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
            Scanner scanner = new Scanner(this.file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(",");

                if (tokens[0].equals(username)) {
                    if (tokens.length == 4) {
                        tokens[3] = this.generateToken();
                        line = String.join(",", tokens);
                    }
                    else {
                        line = String.join(",", tokens);
                        line += "," + this.generateToken();
                    }
                }

                writer.write(line + "\n");
            }

            writer.close();
            scanner.close();

            this.file.delete();
            tempFile.renameTo(this.file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateToken() {
        return this.hash(this.generateRandomWord());
    }

    private String generateRandomWord() {
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            word.append((char)(Math.random() * 26 + 97));
        }
        return word.toString();
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
