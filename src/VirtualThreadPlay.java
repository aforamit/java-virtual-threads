import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class VirtualThreadPlay {

    public static void main(String[] args) throws InterruptedException {
        //example1VirtualThread();
        //example2WithExecutorService();
        example3VirtualThreadWithCarrierThread();
        //example4Synchronization();
    }

    private static void example1VirtualThread() throws InterruptedException {
        Thread.startVirtualThread(
                () -> System.out.println("Project Loom is here!!"));
        Thread.startVirtualThread(
                () -> System.out.println("Project Loom is here2!!"));
        Thread.startVirtualThread(
                () -> System.out.println("Project Loom is here33!!"));

        Thread.sleep(Duration.ofSeconds(3));
        System.out.println("Exiting now...");
    }

    private static void example2WithExecutorService() {
        try (var ex = Executors.newVirtualThreadExecutor()) {
            IntStream.range(0, 5)
                    .forEach(i -> {
                        ex.execute(timeConsumingTask(i));
                    });
        } //AutoCloseable is implemented in ExecutorService.
    }

    private static void example3VirtualThreadWithCarrierThread() {
        timed(o -> {
            int count = 5;
            System.out.println("Hello there " + count);

            //try (var ex = standardSingleExecutorService()) {
            try (var ex = virtualThreadExecutorService()) {
                IntStream.range(0, count)
                        .forEach(i -> ex.execute(timeConsumingTask(i)));
            }
            //ex.shutdown();
            //ex.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        });
    }

    private static Runnable timeConsumingTask(int id) {
        return () -> {
            System.out.println(format("[%s][%s] Starting time consuming task [id=%s]", now(), Thread.currentThread(), id));
            try {
                Thread.sleep(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                System.out.println(format("[%s][%s] InteruptedException occurred time consuming task [id=%s]", now(), Thread.currentThread(), id));
            }
            System.out.println(format("[%s][%s] Ended time consuming task [id=%s]", now(), Thread.currentThread(), id));
        };
    }

    private static ExecutorService standardSingleExecutorService() {
        var factory = Thread.builder().name("standard-thread").daemon(true).factory();
        return Executors.newSingleThreadExecutor(factory);
    }

    private static ExecutorService standardNewExecutorService() {
        var factory = Thread.builder().name("standard-thread").daemon(false).factory();
        return Executors.newThreadExecutor(factory);
    }

    private static ExecutorService virtualThreadExecutorService() {
        var factory = Thread.builder().name("my-carrier").daemon(true).factory();
        //var factory = Thread.builder().name("my-carrier").daemon(false).factory();
        //var factory = Thread.builder().name("virtual-thread", 0).virtual().factory(); //throws IllegalCallerException
        var executor = Executors.newSingleThreadExecutor(factory);

        var virtualThreadFactory = Thread.builder().name("virtual-thread-", 0).virtual(executor).factory();
        //var virtualThreadFactory = Thread.builder().name("virtual-thread", 0).virtual().factory();
        return Executors.newThreadExecutor(virtualThreadFactory).withDeadline(Instant.now().plus(10, ChronoUnit.SECONDS));
    }

    private static void example5() {
        Thread t = new Thread(() -> {
            System.out.println("Hi there");
        });
        t.interrupt();
    }


    private static void example4Synchronization() {
        timed(o -> {
            DataHolder dataHolder = new DataHolder();
            Thread.startVirtualThread(() -> {
                while (true) {
                    System.out.println("Dataholder values: " + dataHolder.getDataMap());
                    sleepForSome(Duration.ofMillis(100));
                }
            });

            try (var ex = virtualThreadExecutorService()) {
            //try (var ex = standardNewExecutorService()) {
                IntStream.range(1, 4)
                        .forEach(i -> {
                            ex.execute(synchroniziedTask(dataHolder, i));
                            System.out.println("Scheduled for : " + i);
                        });
            } //AutoCloseable is implemented in ExecutorService.
            System.out.println("Exiting now...: " + dataHolder.getDataMap());
        });
    }

    private static Runnable synchroniziedTask(final DataHolder dataHolder, int id) {
        return () -> {
            System.out.println(format("[%s][%s] Starting time consuming task [id=%s]", now(), Thread.currentThread(), id));
            Random random = new Random(id * 100L);
            dataHolder.put(id, new ArrayList<>());
            IntStream.range(0, 3).forEach(i -> {
                List<Integer> integers = dataHolder.get(id);
                integers.add(random.nextInt());
                dataHolder.put(id, integers);
                sleepForSome(Duration.ofMillis(5));
            });
            System.out.printf("[%s][%s] Ended time consuming task [id=%s]%n", now(), Thread.currentThread(), id);
        };
    }

    static class DataHolder {
        private final Map<Integer, List<Integer>> dataMap = new HashMap<>();

        public synchronized List<Integer> get(Integer id) {
            System.out.println("Ins the sync getData() for id : " + id);
            List<Integer> integers = dataMap.get(id);
            sleepForSome(Duration.ofMillis(5));
            System.out.println("Out the sync getData() for id : " + id);
            return integers;
        }

        public synchronized void put(Integer id, List<Integer> integers) {
            System.out.println("Ins the sync putData() for id : " + id);
            dataMap.put(id, integers);
            if(id == 2){
                sleepForSome(Duration.ofSeconds(5));
                //sleepForSome(Duration.ofMillis(5));
            } else {
                sleepForSome(Duration.ofMillis(5));
            }
            System.out.println("Out the sync putData() for id : " + id);
        }

        public Map<Integer, List<Integer>> getDataMap() {
            return dataMap;
        }
    }

    private static void sleepForSome(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void timed(Consumer consumer){
        long startTime = System.nanoTime();
        consumer.accept(null);
        long stopTime = System.nanoTime();
        System.out.println(format("[%s][%s] Processing took = %s ms", now
                (), Thread.currentThread(), MILLISECONDS.convert(stopTime - startTime, NANOSECONDS)));
    }
}
