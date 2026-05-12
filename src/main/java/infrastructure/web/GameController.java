package infrastructure.web;

import application.service.CrudGameService;
import application.service.GameService;
import common.Utils;
import domain.Game;
import domain.GameSymbolType;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GameController {
    private final CrudGameService crudGameService;
    private final GameService gameService;
    private final Logger logger;
    private final EventBus eb;

    public GameController(CrudGameService crudGameService, GameService gameService, Logger logger, EventBus eb) {
        this.crudGameService = crudGameService;
        this.gameService = gameService;
        this.logger = logger;
        this.eb = eb;
    }

    public void createNewGame(RoutingContext context) {
        logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
        Game game = crudGameService.createNewGame();
        var reply = new JsonObject();
        reply.put("gameId", game.getId());
        try {
            context.response().setStatusCode(201);
            context.response().putHeader("Location", "/api/games/" + game.getId());
            Utils.sendReply(context.response(), reply);
        } catch (Exception ex) {
            Utils.sendError(context.response());
        }
    }

    public void joinGame(RoutingContext context) {
        logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
        context.request().handler(buf -> {
            JsonObject joinInfo = buf.toJsonObject();
            String userId = context.pathParam("userId");
            String gameId = context.pathParam("gameId");
            String symbol = joinInfo.getString("symbol");
            var reply = new JsonObject();
            try {
                gameService.joinGame(userId, gameId, GameSymbolType.valueOf(symbol.toUpperCase()));
                reply.put("result", "accepted");
                try {
                    Utils.sendReply(context.response(), reply);
                    logger.log(Level.INFO, "Join succeeded");
                } catch (Exception ex) {
                    Utils.sendError(context.response());
                }
            } catch (Exception ex) {
                reply.put("result", "denied");
                try {
                    Utils.sendReply(context.response(), reply);
                    logger.log(Level.INFO, "Join failed");
                } catch (Exception ex2) {
                    Utils.sendError(context.response());
                }
            }
        });
    }

    private String getBusAddressForAGame(String gameId) {
        return "ttt-events-" + gameId;
    }

    public void makeAMove(RoutingContext context) {
        logger.log(Level.INFO, "MakeAMove request - " + context.currentRoute().getPath());
        context.request().handler(buf -> {
            var reply = new JsonObject();
            try {
                JsonObject moveInfo = buf.toJsonObject();
                logger.log(Level.INFO, "move info: " + moveInfo);

                String userId = moveInfo.getString("userId");
                String gameId = context.pathParam("gameId");
                String symbol = moveInfo.getString("symbol");
                int x = getCoordinate(moveInfo, "x");
                int y = getCoordinate(moveInfo, "y");

                var gameSym = toGameSymbol(symbol);
                var game = crudGameService.getGame(gameId);

                gameService.makeAMove(userId, gameId, gameSym, x, y);
                reply.put("result", "accepted");
                try {
                    Utils.sendReply(context.response(), reply);
                } catch (Exception ex) {
                    Utils.sendError(context.response());
                }


                var evMove = new JsonObject();
                evMove.put("event", "new-move");
                evMove.put("x", x);
                evMove.put("y", y);
                evMove.put("symbol", symbol);

                var gameAddress = getBusAddressForAGame(gameId);
                eb.publish(gameAddress, evMove);

                if (game.isGameEnd()) {
                    var evEnd = new JsonObject();
                    evEnd.put("event", "game-ended");
                    if (game.isTie()) {
                        evEnd.put("result", "tie");
                    } else {
                        var sym = game.getWinner().get();
                        if (sym.equals(GameSymbolType.CROSS)) {
                            evEnd.put("winner", "cross");
                        } else {
                            evEnd.put("winner", "circle");
                        }
                    }
                    eb.publish(gameAddress, evEnd);
                }

            } catch (Exception ex) {
                reply.put("result", "invalid-move");
                try {
                    Utils.sendReply(context.response(), reply);
                } catch (Exception ex2) {
                    Utils.sendError(context.response());
                }
            }
        });
    }

    private int getCoordinate(JsonObject moveInfo, String field) {
        Object value = moveInfo.getValue(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private GameSymbolType toGameSymbol(String symbol) {
        return switch (symbol.toLowerCase()) {
            case "cross" -> GameSymbolType.CROSS;
            case "circle" -> GameSymbolType.CIRCLE;
            default -> GameSymbolType.valueOf(symbol.toUpperCase());
        };
    }

    public void handleEventSubscription(HttpServer server, String path) {
        server.webSocketHandler(webSocket -> {
            String gameId = getGameIdFromEventPath(webSocket.path());
            if (gameId == null) {
                webSocket.close();
                return;
            }

            logger.log(Level.INFO, "New TTT subscription accepted.");
            var gameAddress = getBusAddressForAGame(gameId);
            MessageConsumer<Object> consumer = eb.consumer(gameAddress, msg -> {
                JsonObject ev = (JsonObject) msg.body();
                logger.log(Level.INFO, "Notifying event to the frontend: " + ev.encodePrettily());
                webSocket.writeTextMessage(ev.encodePrettily());
            });
            webSocket.closeHandler(done -> consumer.unregister());

            try {
                if (gameService.startGame(gameId)) {
                    var evGameStarted = new JsonObject();
                    evGameStarted.put("event", "game-started");
                    eb.publish(gameAddress, evGameStarted);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private String getGameIdFromEventPath(String path) {
        String prefix = "/api/games/";
        String suffix = "/events";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }
        String gameId = path.substring(prefix.length(), path.length() - suffix.length());
        if (gameId.isBlank() || gameId.contains("/")) {
            return null;
        }
        return gameId;
    }
}
