package common;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class Utils {
    public static void sendReply(HttpServerResponse response, JsonObject reply) {
        response.putHeader("content-type", "application/json");
        response.end(reply.toString());
    }

    public static void sendError(HttpServerResponse response) {
        response.setStatusCode(500);
        response.putHeader("content-type", "application/json");
        response.end();
    }
}
