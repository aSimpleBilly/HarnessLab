package lab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class HarnessLabHub {
    private static final String HTML = "text/html; charset=utf-8";
    private static final String TEXT = "text/plain; charset=utf-8";
    private static final byte[] HEALTHY = "ok\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NOT_FOUND = "Not found\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] METHOD_NOT_ALLOWED = "Method not allowed\n".getBytes(StandardCharsets.UTF_8);

    private HarnessLabHub() {}

    public static void main(String[] args) throws IOException {
        String value = System.getenv().getOrDefault("PORT", "8080");
        int port;
        try {
            port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("PORT must be an integer between 1 and 65535.");
            System.exit(1);
            return;
        }

        HttpServer server = start(new InetSocketAddress(port));
        System.out.println("Harness Lab Hub listening on port " + server.getAddress().getPort());
    }

    static HttpServer start(InetSocketAddress address) throws IOException {
        byte[] page = loadResource("/index.html");
        byte[] canary = loadResource("/canary-hacker.svg");

        HttpServer server = HttpServer.create(address, 0);
        server.createContext("/", exchange -> handle(exchange, page, canary));
        server.start();
        return server;
    }

    private static byte[] loadResource(String path) throws IOException {
        try (InputStream input = HarnessLabHub.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing bundled resource: " + path);
            }
            return input.readAllBytes();
        }
    }

    private static void handle(HttpExchange exchange, byte[] page, byte[] canary) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                send(exchange, 405, TEXT, METHOD_NOT_ALLOWED);
                return;
            }

            switch (exchange.getRequestURI().getRawPath()) {
                case "/" -> send(exchange, 200, HTML, page);
                case "/canary-hacker.svg" -> send(exchange, 200, "image/svg+xml", canary);
                case "/healthz" -> send(exchange, 200, TEXT, HEALTHY);
                default -> send(exchange, 404, TEXT, NOT_FOUND);
            }
        }
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(body.length));
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
