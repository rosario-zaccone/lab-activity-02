package domain.port.out;

import domain.Game;

import java.util.Optional;

public interface GameRepository {
    void save(Game game);
    Optional<Game> get(int id);
    void update(Game game);
}
