package thinking.in.virtual.threads;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static thinking.in.virtual.threads.Utility.timed;

public class VirtualThreadExamples {

    static class Example1ThreadAPI {
        public static void main(String[] args) throws InterruptedException {

            Thread.startVirtualThread(
                    () -> System.out.println("Project Loom is here!! : " + Thread.currentThread()));

            Thread.builder().name("virtual-thread-")
                    .virtual()
                    .task(() -> System.out.println("ThreadBuilder is fun!! : " + Thread.currentThread())).build().start();

            Thread.builder().name("kernel-thread-")
                    .task(() -> System.out.println("ThreadBuilder is fun!! : " + Thread.currentThread())).build().start();

            Thread.sleep(Duration.ofSeconds(1));
            System.out.println("Exiting now...");
        }
    }

    static class Example2CarrierThread {
        public static void main(String[] args) throws InterruptedException {
            var factory = Thread.builder().name("kernel-carrier-thread").daemon(true).factory();
            //var factory = Thread.builder().name("virtual-thread").virtual().factory(); //throws IllegalCallerException

            //this executor creates ONLY 1 kernel thread
            var executor = Executors.newSingleThreadExecutor(factory);

            Thread vThread = Thread.builder().name("virtual-thread-")
                    .virtual(executor)
                    .task(() -> System.out.println("Virtual Thread with Carrier Thread!! : " + Thread.currentThread()))
                    .build();
            vThread.start();

            Thread.sleep(Duration.ofSeconds(1));
            System.out.println("Exiting now...");
        }
    }

    static class Example3KernelThreadBlocking {
        public static void main(String[] args) {
            timed(o -> {
                var factory = Thread.builder().name("kernel-thread").daemon(false).factory();

                try (var ex = Executors.newSingleThreadExecutor(factory)) {
                    IntStream.range(0, 5)
                            .forEach(i -> ex.execute(sleepingTask(i)));
                }
                System.out.println("Exiting now...");
            });
        }
    }

    /**
     *   For more Blocking Operations that are Virtual Thread friendly, see
     *   https://wiki.openjdk.java.net/display/loom/Blocking+Operations
     */
    static class Example3VThreadNonBlocking {
        public static void main(String[] args) {
            timed(o -> {
                ExecutorService kernelThreadExecutor = singleThreadKernelExecutor(); //creates 1 kernel thread

                var virtualThreadFactory = Thread.builder()
                        .name("virtual-thread-", 0).virtual(kernelThreadExecutor).factory();

                //ExecutorService create  a new Virtual Thread for every task
                try (var ex = Executors.newThreadExecutor(virtualThreadFactory)) {
                    IntStream.range(0, 5)
                            .forEach(i -> ex.execute(sleepingTask(i)));
                }
                //ex.shutdown();
                //ex.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                System.out.println("Exiting now...");
            });
        }

        private static ExecutorService singleThreadKernelExecutor() {
            var kernelThreadFactory = Thread.builder().name("kernel-carrier-thread-", 0).daemon(false).factory();
            return Executors.newSingleThreadExecutor(kernelThreadFactory);
        }
    }

    private static Runnable sleepingTask(int id) {
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


}
