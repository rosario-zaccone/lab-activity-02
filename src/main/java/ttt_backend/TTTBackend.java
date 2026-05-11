package ttt_backend;

import java.util.logging.Level;
import java.util.logging.Logger;

import application.port.out.GameRepository;
import application.port.out.UserRepository;
import application.service.CrudGameService;
import application.service.CrudUserService;
import application.service.GameService;
import infrastructure.adapter.persistence.InMemoryGameRepository;
import infrastructure.adapter.web.GameController;
import infrastructure.adapter.web.UserController;
import infrastructure.adapter.persistence.JsonUserRepository;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.StaticHandler;

public class TTTBackend extends VerticleBase {

	static Logger logger = Logger.getLogger("[TicTacToe Backend]");

	private UserRepository userRepository = new JsonUserRepository();
	private CrudUserService crudUserService = new CrudUserService(userRepository);
	private UserController userController = new UserController(crudUserService, logger);

	private GameRepository gameRepository = new InMemoryGameRepository();
	private CrudGameService crudGameService = new CrudGameService(gameRepository);
	private GameService gameService = new GameService(crudUserService, crudGameService);
	private GameController gameController;

	/* port of the endpoint */
	private int port;

	public TTTBackend(int port) {
		this.port = port;
		logger.setLevel(Level.INFO);
	}

	public Future<?> start() {
		logger.log(Level.INFO, "TTT Server initializing...");
		HttpServer server = vertx.createHttpServer();
		gameController = new GameController(crudGameService, gameService, logger, vertx.eventBus());
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/api/registerUser").handler(userController::registerUser);
		router.route(HttpMethod.POST, "/api/createGame").handler(gameController::createNewGame);
		router.route(HttpMethod.POST, "/api/joinGame").handler(gameController::joinGame);
		router.route(HttpMethod.POST, "/api/makeAMove").handler(gameController::makeAMove);

		/* configuring websocket handler */

		gameController.handleEventSubscription(server, "/api/events");

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
	static final int BACKEND_PORT = 8080;

	public static void main(String[] args) {
		var vertx = Vertx.vertx();
		var server = new TTTBackend(BACKEND_PORT);
		vertx.deployVerticle(server);
	}
}
