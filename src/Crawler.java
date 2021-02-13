import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Crawler {
    public static List<URI> links(URI uri) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String input = response.body();
            var patternString = "<a\\s+href\\s*=\\s*(?<quote>['\"])(?<href>[^'\"]*)\\k<quote>";
            Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(input)
                    .results()
                    .map(m -> m.group(2))
                    .filter(s -> s.startsWith("http") || !s.contains(":"))
                    .map(s -> response.uri().resolve(s))
                    .collect(Collectors.toList());
        } catch (IOException | IllegalArgumentException | InterruptedException e) {
            System.out.println("Failed at " + uri);
            return Collections.emptyList();
        }
    }

    static Map<URI, URI> uris = new ConcurrentHashMap<>();

    public static void crawl(URI uri, int depth) throws Throwable {
        if (depth <= 0) return;
        try (var exec = Executors.newVirtualThreadExecutor().withDeadline(Instant.now().plus(30, ChronoUnit.SECONDS))) {
            for (URI u : links(uri)) {
                if (!uris.containsKey(u)) {
                    uris.put(u, u);
                    System.out.println(u);
                    exec.submit(() -> {
                        try {
                            crawl(u, depth - 1);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        crawl(new URI("http://horstmann.com"), 3);
    }
}