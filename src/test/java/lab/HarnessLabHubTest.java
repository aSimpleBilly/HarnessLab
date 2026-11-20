package lab;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HarnessLabHubTest {
    public static void main(String[] args) throws Exception {
        HttpServer server = HarnessLabHub.start(new InetSocketAddress("127.0.0.1", 0));
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> home = request(client, base.resolve("/"), "GET");
            expect(home.statusCode() == 200, "Home should return HTTP 200");
            expect(home.headers().firstValue("Content-Type").orElse("").equals("text/html; charset=utf-8"),
                "Home should serve UTF-8 HTML");
            expect(home.body().contains("Congratulations — you deployed the Harness Lab Hub!"),
                "Home should show the deployment confirmation");
            expect(home.body().contains("href=\"https://developer.harness.io/\""),
                "Home should link to the official developer hub");
            expect(home.body().contains("src=\"/canary-hacker.svg\""),
                "Home should reference the bundled canary image");
            HttpResponse<String> image = request(client, base.resolve("/canary-hacker.svg"), "GET");
            expect(image.statusCode() == 200 && image.body().startsWith("<svg ")
                    && image.headers().firstValue("Content-Type").orElse("").equals("image/svg+xml"),
                "Canary image should be served as SVG from the runnable JAR");

            URI healthUri = base.resolve("/healthz");
            HttpResponse<String> health = request(client, healthUri, "GET");
            expect(health.statusCode() == 200 && health.body().equals("ok\n"),
                "Health should return HTTP 200 with a plain ok response");
            HttpResponse<String> head = request(client, healthUri, "HEAD");
            expect(head.statusCode() == 200 && head.body().isEmpty(), "HEAD should return no body");
            expect(request(client, base.resolve("/healthz/extra"), "GET").statusCode() == 404,
                "The health route should not match nested URLs");
            expect(request(client, healthUri, "POST").statusCode() == 405, "POST should not be allowed");
        } finally {
            server.stop(0);
        }
        System.out.println("Harness Lab Hub HTTP smoke tests passed.");
    }

    private static HttpResponse<String> request(HttpClient client, URI uri, String method)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
            .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
