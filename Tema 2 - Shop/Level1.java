import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class Level1 implements Runnable {
    ExecutorService tpeLevel1;
    ExecutorService tpeLevel2;
    AtomicInteger inQueue1;

    public Level1(ExecutorService tpeLevel1, ExecutorService tpeLevel2, AtomicInteger inQueue1) {
        this.tpeLevel1 = tpeLevel1;
        this.tpeLevel2 = tpeLevel2;
        this.inQueue1 = inQueue1;
    }

    @Override
    public void run() {
        String thisOrder = null;
        int totalProducts = 0;

        try {
            String line;
            synchronized(Tema2.readOrders) {
                line = Tema2.readOrders.readLine(); // se citeste urmatoarea linie din fisierul de comenzi
            }
            if (line != null) { // daca linia citita este null, inseamna ca s-a terminal fisierul
                String[] parse = line.split(","); // se despart numele comenzii de nr de produse
                thisOrder = parse[0];
                totalProducts = Integer.parseInt(parse[1]);

                inQueue1.incrementAndGet();
                tpeLevel1.submit(new Level1(tpeLevel1, tpeLevel2, inQueue1)); // se adauga un task in pool

                AtomicInteger inQueue2 = new AtomicInteger(totalProducts); // pt a contoriza cate produse mai trebuie gasite la level 2
                for (int i = 0; i < totalProducts; i++) {
                    BufferedReader readProducts = new BufferedReader(new FileReader(Tema2.folderName + "/order_products.txt"));
                    tpeLevel2.submit(new Level2(readProducts, inQueue2, thisOrder, i, totalProducts)); // se creeaza cate un task pt fiecare produs
                }
            } else {
                tpeLevel1.shutdown(); // se opreste level 1
                inQueue1.set(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
