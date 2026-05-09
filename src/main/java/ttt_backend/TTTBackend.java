package ttt_backend;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import domain.port.in.UserUseCase;
import application.service.UserService;
import domain.Game;
import domain.GameSymbolType;
import domain.User;
import domain.port.out.UserRepository;
import infrastructure.adapter.in.UserController;
import infrastructure.adapter.in.Utils;
import infrastructure.adapter.out.JsonUserRepository;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.StaticHandler;

public class TTTBackend extends VerticleBase {

	static Logger logger = Logger.getLogger("[TicTacToe Backend]");

	private UserRepository userRepository = new JsonUserRepository();
	private UserUseCase userUseCase = new UserService(userRepository);
	private UserController userController = new UserController(logger, userUseCase);


	/* list on ongoing games */
	private HashMap<String, Game> games;

	/* counter to create game ids */
	private int gamesIdCount;

	/* port of the endpoint */
	private int port;

	public TTTBackend(int port) {
		this.port = port;
		logger.setLevel(Level.INFO);
	}

	public Future<?> start() {
		logger.log(Level.INFO, "TTT Server initializing...");
		HttpServer server = vertx.createHttpServer();

		gamesIdCount = 0;

		games = new HashMap<>();

		/* configuring API routes */

		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/api/registerUser").handler(userController::registerUser);
		router.route(HttpMethod.POST, "/api/createGame").handler(this::createNewGame);
		router.route(HttpMethod.POST, "/api/joinGame").handler(this::joinGame);
		router.route(HttpMethod.POST, "/api/makeAMove").handler(this::makeAMove);

		/* configuring websocket handler */

		handleEventSubscription(server, "/api/events");

		/* enabling access to static files (web app page) */

		router.route("/public/*").handler(StaticHandler.create());


		/* start the server */

		var fut = server
				.requestHandler(router)
				.listen(port);

		fut.onSuccess(res -> {
			logger.log(Level.INFO, "TTT Game Server ready - port: " + port);
		});

		return fut;
	}


	/* List of handlers mapping the API */

	/**
	 * Register a new user
	 */


	/**
	 * Create a New Game
	 */
	protected void createNewGame(RoutingContext context) {
		logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
		gamesIdCount++;
		var newGameId = "game-" + gamesIdCount;
		var game = new Game(newGameId);
		games.put(newGameId, game);
		var reply = new JsonObject();
		reply.put("gameId", newGameId);
		try {
			Utils.sendReply(context.response(), reply);
		} catch (Exception ex) {
			Utils.sendError(context.response());
		}
	}

	/**
	 * Join a Game
	 */
	protected void joinGame(RoutingContext context) {
		logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> {
			JsonObject joinInfo = buf.toJsonObject();
			String userId = joinInfo.getString("userId");
			String gameId = joinInfo.getString("gameId");
			String symbol = joinInfo.getString("symbol");
			var gameSym = symbol.equals("cross") ? GameSymbolType.CROSS : GameSymbolType.CIRCLE;
			var user = userRepository.get(userId).get();
			var game = games.get(gameId);

			var reply = new JsonObject();
			try {
				game.joinGame(user, gameSym);
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

	/**
	 * Make a move in a game
	 */
	protected void makeAMove(RoutingContext context) {
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
				var user = userRepository.get(userId).get();
				var game = games.get(gameId);

				game.makeAmove(user, gameSym, x, y);
				reply.put("result", "accepted");
				try {
					Utils.sendReply(context.response(), reply);
				} catch (Exception ex) {
					Utils.sendError(context.response());
				}

				/* notifying events */

				var eb = vertx.eventBus();

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

	/*
	 * Handling frontend subscriptions to receive events
	 * when joining a game, using websockets
	 */
	protected void handleEventSubscription(HttpServer server, String path) {
		server.webSocketHandler(webSocket -> {
			logger.log(Level.INFO, "New TTT subscription accepted.");
			webSocket.textMessageHandler(openMsg -> {
				logger.log(Level.INFO, "For game: " + openMsg);
				JsonObject obj = new JsonObject(openMsg);
				String gameId = obj.getString("gameId");

				EventBus eb = vertx.eventBus();
				var gameAddress = getBusAddressForAGame(gameId);
				eb.consumer(gameAddress, msg -> {
					JsonObject ev = (JsonObject) msg.body();
					logger.log(Level.INFO, "Notifying event to the frontend: " + ev.encodePrettily());
					webSocket.writeTextMessage(ev.encodePrettily());
				});

				var game = games.get(gameId);
				if (game.bothPlayersJoined()) {
					try {
						game.start();
						var evGameStarted = new JsonObject();
						evGameStarted.put("event", "game-started");
						eb.publish(gameAddress, evGameStarted);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		});
	}

	/* DB management */

	private String getBusAddressForAGame(String gameId) {
		return "ttt-events-" + gameId;
	}

	/* Aux methods */


	static final int BACKEND_PORT = 8080;

	public static void main(String[] args) {
		var vertx = Vertx.vertx();
		var server = new TTTBackend(BACKEND_PORT);
		vertx.deployVerticle(server);
	}
}