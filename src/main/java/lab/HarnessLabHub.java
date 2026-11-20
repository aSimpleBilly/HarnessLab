package lab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class HarnessLabHub {
    private static final String HTML = "text/html; charset=utf-8";
    private static final String TEXT = "text/plain; charset=utf-8";
    private static final String PORT_ERROR = "PORT must be an integer between 1 and 65535.";
    private static final byte[] HEALTHY = "ok\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NOT_FOUND = "Not found\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] METHOD_NOT_ALLOWED = "Method not allowed\n".getBytes(StandardCharsets.UTF_8);

    enum Route { HOME, CANARY, HEALTH, NOT_FOUND, METHOD_NOT_ALLOWED }

    private HarnessLabHub() {}

    public static void main(String[] args) throws IOException {
        int port;
        try {
            port = parsePort(System.getenv("PORT"));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        HttpServer server = start(new InetSocketAddress(port));
        System.out.println("Harness Lab Hub listening on port " + server.getAddress().getPort());
    }

    static int parsePort(String value) {
        if (value == null) {
            return 8080;
        }

        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PORT_ERROR, e);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(PORT_ERROR);
        }
        return port;
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

    static Route route(String method, URI target) {
        if (!method.equals("GET") && !method.equals("HEAD")) {
            return Route.METHOD_NOT_ALLOWED;
        }
        return switch (target.getRawPath()) {
            case "/" -> Route.HOME;
            case "/canary-hacker.svg" -> Route.CANARY;
            case "/healthz" -> Route.HEALTH;
            default -> Route.NOT_FOUND;
        };
    }

    private static void handle(HttpExchange exchange, byte[] page, byte[] canary) throws IOException {
        try (exchange) {
            switch (route(exchange.getRequestMethod(), exchange.getRequestURI())) {
                case HOME -> send(exchange, 200, HTML, page);
                case CANARY -> send(exchange, 200, "image/svg+xml", canary);
                case HEALTH -> send(exchange, 200, TEXT, HEALTHY);
                case NOT_FOUND -> send(exchange, 404, TEXT, NOT_FOUND);
                case METHOD_NOT_ALLOWED -> {
                    exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                    send(exchange, 405, TEXT, METHOD_NOT_ALLOWED);
                }
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
