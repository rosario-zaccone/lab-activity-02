package infrastructure.persistence;

import application.port.out.GameRepository;
import domain.Game;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGameRepository implements GameRepository {
    private static int idCount = 1;

    private final Map<String, Game> store = new ConcurrentHashMap<>();

    @Override
    public Game create() {
        String id = "game-" + idCount;
        idCount += 1;
        var game = new Game(id);
        store.put(id, game);
        return game;


    }

    @Override
    public Optional<Game> get(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void update(Game game) {
        store.put(game.getId(), game);
    }
}