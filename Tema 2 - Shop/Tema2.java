import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema2 {
    public static String folderName;
    public static BufferedReader readOrders;
    public static BufferedWriter printOrders;
    public static BufferedWriter printProducts;

    public static void main(String[] args) throws IOException {
        folderName = args[0]; // primeste argumentele din linia de comanda
        int nrThreads = Integer.parseInt(args[1]);

        readOrders = new BufferedReader(new FileReader(folderName + "/orders.txt"));
        printOrders = new BufferedWriter(new FileWriter("orders_out.txt"));
        printProducts = new BufferedWriter(new FileWriter("order_products_out.txt"));

        AtomicInteger inQueue1 = new AtomicInteger(-1); // pt a contoriza cate task-uri sunt pt level 1
        ExecutorService tpeLevel1 = Executors.newFixedThreadPool(nrThreads); // pt level 1
        ExecutorService tpeLevel2 = Executors.newFixedThreadPool(nrThreads); // pt level 2

        inQueue1.incrementAndGet();
        tpeLevel1.submit(new Level1(tpeLevel1, tpeLevel2, inQueue1)); // se adauga primul task

        while (true) {
            if (tpeLevel1.isTerminated() && inQueue1.get() == -1) { // daca s-a oprit executorul level 1
                tpeLevel2.shutdown(); // se opreste level 2
                break;
            }
        }

        try {
            readOrders.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
