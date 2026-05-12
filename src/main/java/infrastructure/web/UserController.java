package infrastructure.web;

import application.port.in.RegisterUserUseCase;
import application.service.CrudUserService;
import common.Utils;
import domain.User;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UserController {
    private final CrudUserService crudUserService;
    private final Logger logger;

    public UserController(CrudUserService crudUserService, Logger logger) {
        this.crudUserService = crudUserService;
        this.logger = logger;
    }

    public void registerUser(RoutingContext context) {
        logger.log(Level.INFO, "RegisterUser request");
        context.request().handler(buf -> {
            JsonObject userInfo = buf.toJsonObject();
            var userName = userInfo.getString("userName");
            var user = new User(null, userName);
            user = crudUserService.registerUser(user);
            var reply = new JsonObject();
            reply.put("userId", user.id());
            reply.put("userName", user.name());
            try {
                context.response().setStatusCode(201);
                context.response().putHeader("Location", "/api/users/" + user.id());
                Utils.sendReply(context.response(), reply);
            } catch (Exception ex) {
                Utils.sendError(context.response());
            }
        });
    }
}
