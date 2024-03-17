import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import static java.lang.Math.min;

public class MatrixMultiplication {

    public static void main(String[] args) {
        int lin, col;

        Scanner scanner = new Scanner(System.in);

        while (true) {
            int op;

            System.out.println("\n0. Exit");
            System.out.println("1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.println("3. Get Results");
            System.out.print("Selection?: ");

            try {
                op = scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Input must be an integer");
                scanner.nextLine();
                continue;
            }

            if (op == 0)
                break;

            System.out.println("Dimensions?: lins=cols ? ");

            try {
                col = lin = scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Input must be an integer.");
                scanner.nextLine();
                continue;
            }

            switch (op) {
                case 1:
                    OnMult(lin, col, null);
                    break;

                case 2:
                    OnMultLine(lin, col, null);
                    break;

                case 3:
                    try (PrintWriter writer1 = new PrintWriter("java-results-ex1.txt", StandardCharsets.UTF_8);
                         PrintWriter writer2 = new PrintWriter("java-results-ex2.txt", StandardCharsets.UTF_8)) {
                        for (int matrixSize = 600; matrixSize <= 3000; matrixSize += 400) {
                            System.out.println("Size: " + matrixSize + "\n");
                            OnMult(matrixSize, matrixSize, writer1);
                            OnMultLine(matrixSize, matrixSize, writer2);
                        }
                        break;
                    } catch (Exception e) {
                        System.out.println("Error getting results.");
                        return;
                    }

                default:
                    System.out.println("Invalid input");
                    break;
            }
        }
    }

    public static void OnMult(int m_ar, int m_br, PrintWriter writer) {
        double temp;
        int i, j, k;

        double[] pha = new double[m_ar * m_br];
        double[] phb = new double[m_ar * m_br];
        double[] phc = new double[m_ar * m_br];

        for (i=0; i < m_ar; i++) {
            for (j=0; j < m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }
        }

        for (i=0; i < m_br; i++) {
            for (j=0; j < m_br; j++) {
                phb[i*m_br + j] = i+1;
            }
        }

        int counter = 0;
        double start, end;

        if (writer != null) {
            try {
                writer.println("Matrix size: " + m_ar);
            } catch (Exception e) {
                System.out.println("Error writing to file");
                return;
            }
        }

        do {
            System.out.println("Result " + (counter+1));

            start = System.currentTimeMillis();

            for (i = 0; i < m_ar; i++) {
                for (j = 0; j < m_br; j++) {
                    temp = 0;
                    for (k = 0; k < m_ar; k++) {
                        temp += pha[i*m_ar+k] * phb[k*m_br+j];
                    }
                    phc[i*m_ar+j] = temp;
                }
            }

            end = System.currentTimeMillis();

            if (writer != null) {
                try {
                    writer.println((end - start) / 1000);
                } catch (Exception e) {
                    System.out.println("Error writing to file");
                    return;
                }
            }

            System.out.println("Time: " + ((end-start)/1000) + " seconds");

            System.out.println("Result Matrix:");
            for (i = 0; i < 1; i++) {
                for (j = 0; j < min(10,m_br); j++) {
                    System.out.print(phc[j] + " ");
                }
            }

            System.out.println();

            counter++;
        } while(writer != null && counter < 30);
    }

    private static void OnMultLine(int m_ar, int m_br, PrintWriter writer) {
        int i, j, k;

        double[] pha = new double[m_ar * m_br];
        double[] phb = new double[m_ar * m_br];
        double[] phc = new double[m_ar * m_br];

        for (i=0; i < m_ar; i++) {
            for (j=0; j < m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }
        }

        for (i=0; i < m_br; i++) {
            for (j=0; j < m_br; j++) {
                phb[i*m_br + j] = i+1;
            }
        }

        int counter = 0;
        double start, end;

        if (writer != null) {
            try {
                writer.println("Matrix size: " + m_ar);
            } catch (Exception e) {
                System.out.println("Error writing to file");
                return;
            }
        }

        do {
            System.out.println("Result " + (counter+1));

            start = System.currentTimeMillis();

            for (i = 0; i < m_ar; i++) {
                for (k = 0; k < m_br; k++) {
                    for (j = 0; j < m_ar; j++) {
                        phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j];
                    }
                }
            }

            end = System.currentTimeMillis();

            System.out.println("Time: " + ((end-start)/1000) + " seconds");

            System.out.println("Result Matrix:");
            for (i = 0; i < 1; i++) {
                for (j = 0; j < min(10,m_br); j++) {
                    System.out.print(phc[j] + " ");
                }
            }

            if (writer != null) {
                try {
                    writer.println((end - start) / 1000);
                } catch (Exception e) {
                    System.out.println("Error writing to file");
                    return;
                }

                // reset matrix
                for (i = 0; i < m_ar; i++) {
                    phc[i] = 0;
                }
            }

            System.out.println();

            counter++;
        } while (writer != null && counter < 30);
    }
}
