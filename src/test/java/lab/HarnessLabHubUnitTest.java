package lab;

import static lab.HarnessLabHub.Route.*;

import java.net.URI;
import lab.HarnessLabHub.Route;

public final class HarnessLabHubUnitTest {
    private record PortCase(String name, String configured, Integer expected) {}
    private record RouteCase(String method, String target, Route expected) {}

    // A null expected value means the configuration should be rejected.
    private static final PortCase[] PORT_CASES = {
        new PortCase("unset uses default", null, 8080),
        new PortCase("explicit default", "8080", 8080),
        new PortCase("custom port", "9090", 9090),
        new PortCase("lowest allowed port", "1", 1),
        new PortCase("highest allowed port", "65535", 65535),
        new PortCase("leading zero is decimal", "08080", 8080),
        new PortCase("explicit positive sign", "+8080", 8080),
        new PortCase("zero would select an ephemeral port", "0", null),
        new PortCase("negative port", "-1", null),
        new PortCase("one above the highest port", "65536", null),
        new PortCase("very large integer", "2147483647", null),
        new PortCase("integer overflow", "2147483648", null),
        new PortCase("empty is not unset", "", null),
        new PortCase("blank is not unset", " ", null),
        new PortCase("leading whitespace", " 8080", null),
        new PortCase("trailing newline", "8080\n", null),
        new PortCase("fractional port", "80.80", null),
        new PortCase("hexadecimal notation", "0x1F90", null),
        new PortCase("Docker port/protocol notation", "8080/tcp", null),
        new PortCase("unexpanded environment variable", "$PORT", null),
    };

    private static final RouteCase[] ROUTE_CASES = {
        new RouteCase("GET", "/", HOME),
        new RouteCase("HEAD", "/", HOME),
        new RouteCase("GET", "/healthz", HEALTH),
        new RouteCase("HEAD", "/healthz", HEALTH),
        new RouteCase("GET", "/canary-hacker.svg", CANARY),
        new RouteCase("HEAD", "/canary-hacker.svg", CANARY),
        new RouteCase("GET", "/?next=/healthz", HOME),
        new RouteCase("GET", "/healthz?probe=ready", HEALTH),
        new RouteCase("GET", "/canary-hacker.svg?v=1", CANARY),
        new RouteCase("GET", "/unknown", NOT_FOUND),
        new RouteCase("HEAD", "/unknown", NOT_FOUND),
        new RouteCase("GET", "/healthz/", NOT_FOUND),
        new RouteCase("GET", "/canary-hacker.svg/", NOT_FOUND),
        new RouteCase("GET", "/healthz/ready", NOT_FOUND),
        new RouteCase("GET", "/healthz-old", NOT_FOUND),
        new RouteCase("GET", "/canary-hacker.svg.bak", NOT_FOUND),
        new RouteCase("GET", "/Healthz", NOT_FOUND),
        new RouteCase("GET", "/canary-hacker.SVG", NOT_FOUND),
        new RouteCase("GET", "/%68ealthz", NOT_FOUND),
        new RouteCase("GET", "/healthz%2F", NOT_FOUND),
        new RouteCase("GET", "/./healthz", NOT_FOUND),
        new RouteCase("GET", "http://lab.example/healthz", HEALTH),
        new RouteCase("GET", "http://lab.example", NOT_FOUND),
        new RouteCase("POST", "/", METHOD_NOT_ALLOWED),
        new RouteCase("PUT", "/healthz", METHOD_NOT_ALLOWED),
        new RouteCase("DELETE", "/canary-hacker.svg", METHOD_NOT_ALLOWED),
        new RouteCase("OPTIONS", "*", METHOD_NOT_ALLOWED),
        new RouteCase("TRACE", "/", METHOD_NOT_ALLOWED),
        new RouteCase("get", "/", METHOD_NOT_ALLOWED),
        new RouteCase("POST", "/missing", METHOD_NOT_ALLOWED),
    };

    public static void main(String[] args) {
        for (PortCase test : PORT_CASES) {
            checkPort(test);
        }
        for (RouteCase test : ROUTE_CASES) {
            Route actual = HarnessLabHub.route(test.method(), URI.create(test.target()));
            if (actual != test.expected()) {
                throw new AssertionError(test.method() + " " + test.target()
                    + ": expected " + test.expected() + ", got " + actual);
            }
        }
        System.out.printf("%d unit tests passed (%d port configuration, %d request routing).%n",
            PORT_CASES.length + ROUTE_CASES.length, PORT_CASES.length, ROUTE_CASES.length);
    }

    private static void checkPort(PortCase test) {
        int actual;
        try {
            actual = HarnessLabHub.parsePort(test.configured());
        } catch (IllegalArgumentException e) {
            if (test.expected() == null) {
                return;
            }
            throw new AssertionError(test.name() + ": expected port " + test.expected(), e);
        }
        if (test.expected() == null || actual != test.expected()) {
            throw new AssertionError(test.name() + ": expected "
                + (test.expected() == null ? "rejection" : "port " + test.expected()) + ", got " + actual);
        }
    }
}
