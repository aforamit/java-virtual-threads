package thinking.in.virtual.threads;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Was expecting the VirtualThread to show stacktrace hierarchy,
 * but it did not work as expected :-(
 */

public class VirtualThreadExceptionsExample {

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(sleepTask(1));
        t.start();
        System.out.println(Thread.currentThread());
//        try{
//            throw new RuntimeException("ho ho");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        example3VirtualThreadWithCarrierThread();
    }

    private static void example3VirtualThreadWithCarrierThread() {
        timed(o -> {
            int count = 2;
            System.out.println("Hello there " + count);

            List<Thread> threads = IntStream.range(0, count)
                    .mapToObj(i -> makeVThread(i, newThreadTask(i))).collect(Collectors.toList());
            threads.forEach(t -> t.start());
            try {
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            threads.forEach(t -> t.interrupt());

        });
    }

    private static Runnable newThreadTask(int id) {
        return () -> {
            System.out.println(format("[%s][%s] Starting time newThread task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, id));
            try (var ex = virtualThreadExecutorServiceChild(id)) {
                ex.execute(sleepTask(id));
            }
            //
            // sleepTask(id).run();

            System.out.println(format("[%s][%s] Endedd time newThread task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, id));
        };
    }

    private static Runnable sleepTask(int id) {
        return () -> {
            System.out.println(format("[%s][%s] Starting time sleep task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, id));
            try {
                Thread.sleep(Duration.ofSeconds(10));
            } catch (InterruptedException e) {
                System.out.println(format("[%s][%s] InteruptedException occurred time sleep task [id=%s] : [i=%s]",
                        now(), Thread.currentThread(), id, id));
                e.printStackTrace();
            }
            System.out.println(format("[%s][%s] Ended time sleep task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, id));
        };
    }


    private static Runnable exceptionThrowingTasks(int id) {
        return () -> {
            System.out.println(format("[%s][%s] Starting time consuming task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, id));
            if (id == 2) {
                throw new RuntimeException("Oops! I did it again");
            }
            System.out.println(format("[%s][%s] Ended time consuming task [id=%s] : [i=%s]", now(), Thread.currentThread(), id, id));
        };
    }

    private static ExecutorService standardSingleExecutorService() {
        //var factory = Thread.builder().name("standard-thread").daemon(true).factory();
        //return Executors.newSingleThreadExecutor(factory);
        var factory = Thread.builder().name("standard-thread").daemon(false).factory();
        return Executors.newThreadExecutor(factory);
    }

    public static Thread makeThread(Runnable r) {
        //return Thread.builder().virtual().task(r).build();
        return Thread.builder().task(r).build();
    }

    public static Thread makeVThread(int id, Runnable r) {
        var factory = Thread.builder().name("my-carrier").daemon(false).factory();
        var executor = Executors.newSingleThreadExecutor(factory);
        return Thread.builder().name("virtual-thread-", id).virtual(executor).task(r).build();
    }

    public static Thread makeVThreadChild(int id, Runnable r) {
        return Thread.builder().name("virtual-thread-child-", id).virtual().task(r).build();
    }

    private static ExecutorService virtualThreadExecutorService() {
        var factory = Thread.builder().name("my-carrier").daemon(true).factory();
        var executor = Executors.newSingleThreadExecutor(factory);

        var virtualThreadFactory = Thread.builder().name("virtual-thread-", 0).virtual(executor).factory();
        return Executors.newThreadExecutor(virtualThreadFactory);
    }

    private static ExecutorService virtualThreadExecutorServiceChild(int id) {
        var factory = Thread.builder().name("my-carrier-child").daemon(true).factory();
        var executor = Executors.newSingleThreadExecutor(factory);

        var virtualThreadFactory = Thread.builder().name("virtual-thread-child", id).virtual(executor).factory();
        return Executors.newThreadExecutor(virtualThreadFactory);
    }

    private static void timed(Consumer consumer) {
        long startTime = System.nanoTime();
        consumer.accept(null);
        long stopTime = System.nanoTime();
        System.out.println(format("[%s][%s] Processing took = %s ms", now
                (), Thread.currentThread(), MILLISECONDS.convert(stopTime - startTime, NANOSECONDS)));
    }
}
