package thinking.in.virtual.threads;

import java.util.function.Consumer;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Utility {

    public static void timed(Consumer consumer) {
        long startTime = System.nanoTime();
        consumer.accept(null);
        long stopTime = System.nanoTime();
        System.out.println(format("[%s][%s] Processing took = %s ms", now
                (), Thread.currentThread(), MILLISECONDS.convert(stopTime - startTime, NANOSECONDS)));
    }

}
