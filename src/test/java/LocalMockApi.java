import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

final class LocalMockApi {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<Long> DELETED_IDS = ConcurrentHashMap.newKeySet();
    private static HttpServer server;
    private static String baseUri;

    private LocalMockApi() {
    }

    static synchronized String baseUri() {
        if (server == null) {
            start();
        }
        return baseUri;
    }

    private static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", LocalMockApi::handle);
            server.setExecutor(Executors.newCachedThreadPool(task -> {
                Thread thread = new Thread(task, "local-mock-api");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            baseUri = "http://127.0.0.1:" + server.getAddress().getPort();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start local mock API", e);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/health") && method.equals("GET")) {
                sendJson(exchange, 200, Map.of("status", "UP", "service", "local-mock-api"));
            } else if (path.equals("/items")) {
                handleItems(exchange, method);
            } else if (path.startsWith("/items/")) {
                handleItem(exchange, method, path.substring("/items/".length()));
            } else if (path.equals("/headers") && method.equals("GET")) {
                handleHeaders(exchange);
            } else if (path.equals("/auth") && method.equals("GET")) {
                handleAuth(exchange);
            } else if (path.equals("/validate") && method.equals("POST")) {
                handleValidation(exchange);
            } else if (path.equals("/idempotency") && method.equals("POST")) {
                handleIdempotency(exchange);
            } else if (path.startsWith("/errors/") && method.equals("GET")) {
                handleError(exchange, path.substring("/errors/".length()));
            } else {
                sendError(exchange, 404, "route_not_found", "No route matches the request");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "bad_request", e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, "internal_error", "Unexpected mock server error");
        } finally {
            exchange.close();
        }
    }

    private static void handleItems(HttpExchange exchange, String method) throws IOException {
        if (method.equals("GET")) {
            Map<String, String> query = query(exchange);
            int page = integer(query.getOrDefault("page", "1"), "page");
            int size = integer(query.getOrDefault("size", "20"), "size");
            if (page < 1 || size < 1 || size > 100) {
                sendError(exchange, 400, "invalid_pagination", "page must be positive and size must be 1..100");
                return;
            }
            String sort = query.getOrDefault("sort", "id");
            String order = query.getOrDefault("order", "asc");
            String status = query.getOrDefault("status", "all");
            String category = query.getOrDefault("category", "all");
            sendJson(exchange, 200, Map.of(
                    "page", page, "size", size, "sort", sort, "order", order,
                    "status", status, "category", category, "offset", (page - 1) * size));
            return;
        }
        if (method.equals("POST")) {
            Map<String, Object> body = body(exchange);
            String name = string(body.get("name"));
            if (name == null || name.isBlank()) {
                sendError(exchange, 400, "invalid_name", "name is required");
            } else if (name.length() > 64) {
                sendError(exchange, 422, "name_too_long", "name must contain at most 64 characters");
            } else if (name.startsWith("duplicate-")) {
                sendError(exchange, 409, "duplicate_item", "name already exists");
            } else {
                long id = positiveHash(name);
                DELETED_IDS.remove(id);
                sendJson(exchange, 201, Map.of("id", id, "name", name,
                        "status", stringOrDefault(body.get("status"), "active")));
            }
            return;
        }
        sendError(exchange, 405, "method_not_allowed", "Unsupported items method");
    }

    private static void handleItem(HttpExchange exchange, String method, String rawId) throws IOException {
        long id;
        try {
            id = Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "invalid_id", "id must be numeric");
            return;
        }
        if (id <= 0) {
            sendError(exchange, 400, "invalid_id", "id must be positive");
            return;
        }
        if (id >= 900_000 || DELETED_IDS.contains(id)) {
            sendError(exchange, 404, "item_not_found", "Item does not exist");
            return;
        }

        if (method.equals("GET")) {
            sendJson(exchange, 200, Map.of("id", id, "name", "item-" + id, "status", "active"));
        } else if (method.equals("PUT")) {
            Map<String, Object> body = body(exchange);
            String name = string(body.get("name"));
            String status = string(body.get("status"));
            if (name == null || name.isBlank() || status == null || status.isBlank()) {
                sendError(exchange, 400, "invalid_replacement", "name and status are required");
            } else {
                sendJson(exchange, 200, Map.of("id", id, "name", name, "status", status, "replaced", true));
            }
        } else if (method.equals("PATCH")) {
            Map<String, Object> body = body(exchange);
            if (body.isEmpty() || (!body.containsKey("name") && !body.containsKey("status"))) {
                sendError(exchange, 400, "invalid_patch", "name or status is required");
            } else {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("id", id);
                response.put("patched", true);
                if (body.containsKey("name")) response.put("name", body.get("name"));
                if (body.containsKey("status")) response.put("status", body.get("status"));
                sendJson(exchange, 200, response);
            }
        } else if (method.equals("DELETE")) {
            DELETED_IDS.add(id);
            exchange.sendResponseHeaders(204, -1);
        } else {
            sendError(exchange, 405, "method_not_allowed", "Unsupported item method");
        }
    }

    private static void handleHeaders(HttpExchange exchange) throws IOException {
        String correlationId = exchange.getRequestHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            sendError(exchange, 400, "missing_header", "X-Correlation-ID is required");
            return;
        }
        String language = exchange.getRequestHeaders().getFirst("Accept-Language");
        sendJson(exchange, 200, Map.of("correlationId", correlationId,
                "language", language == null ? "unspecified" : language));
    }

    private static void handleAuth(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null) {
            sendError(exchange, 401, "missing_token", "Bearer token is required");
        } else if (!auth.matches("Bearer token-\\d+")) {
            sendError(exchange, 403, "invalid_token", "Bearer token is invalid");
        } else {
            sendJson(exchange, 200, Map.of("authenticated", true, "subject", auth.substring(7)));
        }
    }

    private static void handleValidation(HttpExchange exchange) throws IOException {
        Map<String, Object> body = body(exchange);
        String value = string(body.get("value"));
        if (value == null) {
            sendError(exchange, 400, "missing_value", "value is required");
        } else if (value.length() < 2 || value.length() > 50) {
            sendError(exchange, 422, "invalid_length", "value length must be 2..50");
        } else {
            sendJson(exchange, 200, Map.of("valid", true, "value", value, "length", value.length()));
        }
    }

    private static void handleIdempotency(HttpExchange exchange) throws IOException {
        String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        if (key == null || key.isBlank()) {
            sendError(exchange, 400, "missing_idempotency_key", "Idempotency-Key is required");
        } else {
            sendJson(exchange, 201, Map.of("operationId", positiveHash(key), "key", key));
        }
    }

    private static void handleError(HttpExchange exchange, String rawCode) throws IOException {
        int code = integer(rawCode, "status code");
        if (!Set.of(400, 401, 403, 404, 405, 409, 415, 422, 429).contains(code)) {
            sendError(exchange, 400, "unsupported_error_code", "Unsupported error status");
            return;
        }
        sendError(exchange, code, "error_" + code, "Deterministic error " + code);
    }

    private static Map<String, Object> body(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) return Map.of();
        try {
            return JSON.readValue(bytes, new TypeReference<>() { });
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body must be valid JSON");
        }
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> values = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return values;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
        }
        return values;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int integer(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be numeric");
        }
    }

    private static long positiveHash(String value) {
        return Integer.toUnsignedLong(value.hashCode()) + 1;
    }

    private static String string(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static String stringOrDefault(Object value, String fallback) {
        String text = string(value);
        return text == null ? fallback : text;
    }

    private static void sendError(HttpExchange exchange, int status, String code, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", code, "message", message, "status", status));
    }

    private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("X-Mock-API", "local");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
