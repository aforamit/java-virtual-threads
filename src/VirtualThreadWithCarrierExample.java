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

public class VirtualThreadWithCarrierExample {

    public static void main(String[] args) throws InterruptedException {
        //example1VirtualThread();
        //example2WithExecutorService();
        example3VirtualThreadWithCarrierThread();
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
            IntStream.range(0,6).forEach(i -> {
                System.out.println(format("[%s][%s] Starting time consuming task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, i));
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException e) {
                    System.out.println(format("[%s][%s] InteruptedException occurred time consuming task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, i));
                }
                System.out.println(format("[%s][%s] Ended time consuming task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, i));
            });
        };
    }

    private static ExecutorService standardSingleExecutorService() {
        //var factory = Thread.builder().name("standard-thread").daemon(true).factory();
        //return Executors.newSingleThreadExecutor(factory);
        var factory = Thread.builder().name("standard-thread").daemon(false).factory();
        return Executors.newThreadExecutor(factory);
    }

    private static ExecutorService virtualThreadExecutorService() {
        var factory = Thread.builder().name("my-carrier").daemon(true).factory();
        //var factory = Thread.builder().name("my-carrier").daemon(false).factory();
        //var factory = Thread.builder().name("virtual-thread", 0).virtual().factory(); //throws IllegalCallerException
        var executor = Executors.newSingleThreadExecutor(factory);

        //var virtualThreadFactory = Thread.builder().name("virtual-thread-", 0).virtual(executor).factory();

        ///default executor - ForkJoinPool
        var virtualThreadFactory = Thread.builder().name("virtual-thread-", 0).virtual().factory();

        return Executors.newThreadExecutor(virtualThreadFactory).withDeadline(Instant.now().plus(20, ChronoUnit.SECONDS));
    }

    private static void timed(Consumer consumer){
        long startTime = System.nanoTime();
        consumer.accept(null);
        long stopTime = System.nanoTime();
        System.out.println(format("[%s][%s] Processing took = %s ms", now
                (), Thread.currentThread(), MILLISECONDS.convert(stopTime - startTime, NANOSECONDS)));
    }
}
