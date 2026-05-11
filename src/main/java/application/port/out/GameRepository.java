package application.port.out;

import domain.Game;

import java.util.Optional;

public interface GameRepository {
    Game create();
    Optional<Game> get(String id);
    void update(Game game);
}
