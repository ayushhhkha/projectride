import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemoryBenchmark {

    private static final int[] DRIVER_COUNTS = {100_000, 500_000, 1_000_000};

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("%-12s %-20s %-20s %-14s%n","Drivers", "AoS heap (MB)", "SoA heap (MB)", "Reduction");
        System.out.println("-".repeat(68));

        for (int count : DRIVER_COUNTS) {
            long aosBytes = measureAos(count);
            long soaBytes = measureSoa(count);

            double aosMb = aosBytes / (1024.0 * 1024.0);
            double soaMb = soaBytes / (1024.0 * 1024.0);
            double reductionPercent = 100.0 * (1 - (soaMb / aosMb));

            System.out.printf("%-12d %-20.1f %-20.1f %-14.1f%%%n",count, aosMb, soaMb, reductionPercent);
        }

        System.out.println();
        System.out.println("Note: measured via Runtime.totalMemory() - freeMemory(), with System.gc()");
        System.out.println("requested before each measurement. JVM heap accounting is");
        System.out.println("approximate -- treat these as directionally accurate, not exact byte counts.");
    }

    private static long measureAos(int count) throws InterruptedException {
        Random random = new Random(5);
        System.gc();
        Thread.sleep(100);
        long before = usedMemory();

        List<Driver> drivers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            drivers.add(new Driver(i, new Location(52.0 + random.nextDouble() * 0.5, 4.2 + random.nextDouble() * 0.8)));
        }

        System.gc();
        Thread.sleep(100);
        long after = usedMemory();

        long keepAlive = drivers.size();
        return (after - before) + (keepAlive * 0);
    }

    private static long measureSoa(int count) throws InterruptedException {
        Random random = new Random(5);
        System.gc();
        Thread.sleep(100);
        long before = usedMemory();

        double[] latitudes = new double[count];
        double[] longitudes = new double[count];
        boolean[] available = new boolean[count];
        int[] ids = new int[count];

        for (int i = 0; i < count; i++) {
            latitudes[i] = 52.0 + random.nextDouble() * 0.5;
            longitudes[i] = 4.2 + random.nextDouble() * 0.8;
            available[i] = true;
            ids[i] = i;
        }

        System.gc();
        Thread.sleep(100);
        long after = usedMemory();

        long keepAlive = latitudes.length + longitudes.length + available.length + ids.length;
        return (after - before) + (keepAlive * 0);
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}