package application.port.in;

import application.GameNotFoundException;
import domain.Game;

public interface GetGameUseCase {
    Game getGame(String id) throws GameNotFoundException;
}
