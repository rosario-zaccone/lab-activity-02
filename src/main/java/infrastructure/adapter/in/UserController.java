package infrastructure.adapter.in;

import domain.User;
import domain.port.in.UserUseCase;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UserController {
    private final UserUseCase userUseCase;
    private final Logger logger;

    public UserController(Logger logger, UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
        this.logger = logger;
    }

    public void registerUser(RoutingContext context) {
        logger.log(Level.INFO, "RegisterUser request");
        context.request().handler(buf -> {
            JsonObject userInfo = buf.toJsonObject();
            var userName = userInfo.getString("userName");
            var user = new User(null, userName);
            user = userUseCase.registerUser(user);
            var reply = new JsonObject();
            reply.put("userId", user.id());
            reply.put("userName", user.name());
            try {
                Utils.sendReply(context.response(), reply);
            } catch (Exception ex) {
                Utils.sendError(context.response());
            }
        });
    }
}
