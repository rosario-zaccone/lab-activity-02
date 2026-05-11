package application.service;

import application.GameNotFoundException;
import application.port.in.CreateGameUseCase;
import application.port.in.GetGameUseCase;
import application.port.out.GameRepository;
import domain.Game;


public class CrudGameService implements CreateGameUseCase, GetGameUseCase {
    private final GameRepository gameRepository;
    public CrudGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Game createNewGame() {
        return gameRepository.create();
    }

    @Override
    public Game getGame(String id)  {
        var game =  gameRepository.get(id);
        if (game.isEmpty())
            throw new GameNotFoundException(id);
        else
            return game.get();
    }
}
