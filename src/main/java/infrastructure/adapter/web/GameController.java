package infrastructure.adapter.web;

import application.service.CrudGameService;
import application.service.GameService;
import common.Utils;
import domain.Game;
import domain.GameSymbolType;
import io.vertx.core.eventbus.EventBus;
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
            Utils.sendReply(context.response(), reply);
        } catch (Exception ex) {
            Utils.sendError(context.response());
        }
    }

    public void joinGame(RoutingContext context) {
        logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
        context.request().handler(buf -> {
            JsonObject joinInfo = buf.toJsonObject();
            String userId = joinInfo.getString("userId");
            String gameId = joinInfo.getString("gameId");
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
                String gameId = moveInfo.getString("gameId");
                String symbol = moveInfo.getString("symbol");
                int x = Integer.parseInt(moveInfo.getString("x"));
                int y = Integer.parseInt(moveInfo.getString("y"));

                var gameSym = symbol.equals("cross") ? GameSymbolType.CROSS : GameSymbolType.CIRCLE;
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

    public void handleEventSubscription(HttpServer server, String path) {
        server.webSocketHandler(webSocket -> {
            logger.log(Level.INFO, "New TTT subscription accepted.");
            webSocket.textMessageHandler(openMsg -> {
                logger.log(Level.INFO, "For game: " + openMsg);
                JsonObject obj = new JsonObject(openMsg);
                String gameId = obj.getString("gameId");

                var gameAddress = getBusAddressForAGame(gameId);
                eb.consumer(gameAddress, msg -> {
                    JsonObject ev = (JsonObject) msg.body();
                    logger.log(Level.INFO, "Notifying event to the frontend: " + ev.encodePrettily());
                    webSocket.writeTextMessage(ev.encodePrettily());
                });

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
        });
    }
}
