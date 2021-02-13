import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

public class TempTest {

    public static void main(String[] args) throws InterruptedException, IOException {
        Thread thread = new Thread(() -> {
            int a = 2, b = 4;
            int c = a + b;
            try {
                Thread.sleep(1000l); //cpu idle //same with other blocking IO operations
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int d = a * b;
        });
        thread.start();

        InputStream inputStream = null;
        inputStream.read();

        Writer writer = null;
        writer.write("Hello Thread!");

        Semaphore semaphore = null;
        semaphore.acquire();
    }

    static class SyncFuncActivate {
        SyncDatabase database = null;

        boolean activateUser(Long userId) {
            User user = database.findUser(userId);
            if (user != null && !user.isActive()) {
                database.activateUser(userId);
                return true;
            } else {
                return false;
            }
        }

    }

    static class SyncFuncUpdateAddress {
        SyncDatabase myDB = null;
        boolean updateAddress(Long userId, String address) {
            User user = myDB.findUser(userId);
            if (user != null) {
                myDB.updateAddress(userId, address);
                return true;
            } else {
                return false;
            }
        }
    }

    static class SyncDatabase {
        private void activateUser(Long userId) {
        }

        private void updateAddress(Long userId, String address) {
        }

        private User findUser(Long userId) {
            return null;
        }
    }

    static class AsyncFunc {
        AsyncDatabase database = null;

        CompletableFuture<Boolean> activateUser(Long userId) {
            return database.findUser(userId).thenCompose(user -> {
                if (user != null && !user.isActive()) {
                    return database.activateUser(userId).thenApply(r -> true);
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            });
        }
    }

    static class AsyncFuncUpdateAddress {
        AsyncDatabase myDB = null;

        CompletableFuture<Boolean> updateAddress(Long userId, String address) {
            return myDB.findUser(userId).thenCompose(user -> {
                if (user != null) {
                    return myDB.updateAddress(userId, address).thenApply(r -> true);
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            });
        }
    }

    static class AsyncDatabase {
        private CompletableFuture<User> activateUser(Long userId) {
            return null;
        }

        private CompletableFuture<User> updateAddress(Long userId, String address){
            return null;
        }

        private CompletableFuture<User> findUser(Long userId) {
            return null;
        }
    }

    static class User {
        public boolean isActive() {
            return false;
        }
    }

    static class Http {
        CompletableFuture<String> handle(HttpRequest request) {
            return HttpClient.newBuilder()
                    .build()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {})
                    .thenApply(response -> "")
                    .exceptionally(e -> "");
        }
    }
}
