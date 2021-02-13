package thinking.in.virtual.threads;

public class ContinuationExamples {

    public static void main(String[] args) throws InterruptedException {
        var scope = new ContinuationScope("C1");
        var continuation = new Continuation(scope, () -> {
            try {
                System.out.println("Continuation : Start " + scope.getName());
                Continuation.yield(scope);
                System.out.println("Continuation : Middle " + scope.getName());
                Continuation.yield(scope);
                System.out.println("Continuation : End " + scope.getName());
            } finally {
                System.out.println("Finally called now " + scope.getName());
            }
        });

        System.out.println("Continuation.run() 1");
        continuation.run();
        System.out.println("Continuation.run() 2");
        continuation.run();
        System.out.println("Continuation.run() 3");
        continuation.run();
        System.out.println("Exiting now....");
    }
}
