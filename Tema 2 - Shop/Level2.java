import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class Level2 implements Runnable {
    final BufferedReader readProducts;
    AtomicInteger inQueue2;
    String orderThatGenerated;
    int pozProd;
    int totalProducts;

    public Level2(BufferedReader readProducts, AtomicInteger inQueue2, String orderThatGenerated, int pozProd, int totalProducts) {
        this.readProducts = readProducts;
        this.inQueue2 = inQueue2;
        this.orderThatGenerated = orderThatGenerated;
        this.pozProd = pozProd;
        this.totalProducts = totalProducts;
    }

    @Override
    public void run() {
        String thisOrder = null;
        String product = null;
        AtomicInteger nrProdFound = new AtomicInteger(0); // se contorizeaza cate produse din comanda au fost gasite

        while (nrProdFound.get() < totalProducts) { // citeste cate o linie pana cand nu sunt gasite toate produsele
            try {
                String line;
                synchronized (readProducts) {
                    line = readProducts.readLine();
                }

                if (line != null) { // daca linia citita este null, inseamna ca s-a terminal fisierul
                    String[] parse = line.split(","); // se despart numele comenzii de nr de produse
                    thisOrder = parse[0];
                    product = parse[1];

                    if (thisOrder.equals(orderThatGenerated)) { // daca produsul curent este din comanda ceruta
                        nrProdFound.incrementAndGet(); // se incrementeaza nr de produse gasite
                        if (nrProdFound.get() == pozProd + 1 && thisOrder.equals(orderThatGenerated)) { // nr produsul gasit este acelasi cu produsul cautat
                            synchronized (Tema2.printProducts) { // se afiseaza in fisierul de produse
                                Tema2.printProducts.write(thisOrder + "," + product + ",shipped\n");
                                Tema2.printProducts.flush();
                                inQueue2.decrementAndGet(); // produs rezolvat, se scoate din pool
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (nrProdFound.get() == totalProducts && inQueue2.get() == 0) { // s-au gasit toate produsele
            synchronized (Tema2.printOrders) {
                if (inQueue2.get() == 0) {
                    try { // comanda rezolvata, se afiseaza in fisierul de comenzi
                        Tema2.printOrders.write(orderThatGenerated + "," + totalProducts + ",shipped\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Tema2.printOrders.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            inQueue2.decrementAndGet();
        }

        try {
            readProducts.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
