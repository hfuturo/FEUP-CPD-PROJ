package Game.Words;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Words {

    private final static String RAW_FILE_PATH = "Game/Words/raw_words.txt";
    private final static String FILE_PATH = "Game/Words/words.txt";
    private final File raw_file;
    private final File file;

    public Words() throws NoSuchFileException {
        this.raw_file = new File(Words.RAW_FILE_PATH);
        this.file = new File(Words.FILE_PATH);

        this.run();
    }

    private void run() throws NoSuchFileException {
        try {
            if (!this.file.createNewFile()) return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!this.raw_file.exists())
            throw new NoSuchFileException(Words.RAW_FILE_PATH);

        try (Scanner scanner = new Scanner(this.raw_file);
             BufferedWriter fileWriter = new BufferedWriter(new FileWriter(this.file))) {

            while (scanner.hasNextLine()) {
                String word = scanner.nextLine().trim().toLowerCase();

                if (word.length() != 5) continue;
                if (!this.allLeters(word)) continue;

                fileWriter.write(word);
                fileWriter.write("\n");
            }

        } catch (Exception e) {
            throw new RuntimeException("Error sanitizing file.");
        }
    }

    private boolean allLeters(String word) {
        for (char c : word.toCharArray()) {
            if (!Character.isLetter(c))
                return false;
        }
        return true;
    }

    public String getRandomWord() {
        try (Scanner scanner = new Scanner(this.file)) {
            List<String> words = new ArrayList<>();

            while (scanner.hasNextLine())
                words.add(scanner.nextLine());

            return words.get((int)(Math.random() * words.size()));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
