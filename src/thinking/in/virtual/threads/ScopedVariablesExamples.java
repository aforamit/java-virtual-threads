package thinking.in.virtual.threads;

public class ScopedVariablesExamples {

    static final Scoped<String> scoped = Scoped.forType(String.class);
    static final Scoped<Integer> scoped2 = Scoped.forType(Integer.class);

    public static void main(String[] args) {
        ScopedVariablesExamples play = new ScopedVariablesExamples();
        play.foo();
    }

    void foo() {
        try (var __ = scoped.bind("AA")) {
            bar();
            baz();
            bar();
        }
    }

    private void bar() {
        System.out.println("" + scoped.get());
    }

    private void baz() {
        try (var __ = scoped.bind("B")) {
            bar();
        }
    }

}
