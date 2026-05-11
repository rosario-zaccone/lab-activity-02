package application.service;

import application.port.in.JoinGameUseCase;
import application.port.in.MakeAMoveUseCase;
import application.port.in.StartGameUseCase;
import domain.GameSymbolType;
import application.InvalidJoinException;
import ttt_backend.InvalidMoveException;

public class GameService implements JoinGameUseCase, MakeAMoveUseCase, StartGameUseCase {
    private final CrudUserService crudUserService;
    private final CrudGameService crudGameService;

    public GameService(CrudUserService crudUserService, CrudGameService crudGameService) {
        this.crudUserService = crudUserService;
        this.crudGameService = crudGameService;
    }

    @Override
    public void joinGame(String userId, String gameId, GameSymbolType symbol) throws InvalidJoinException {
        var user = crudUserService.getUser(userId);
        var game = crudGameService.getGame(gameId);
        game.joinGame(user, symbol);
    }

    @Override
    public void makeAMove(String userId, String gameId, GameSymbolType gameSymbol, int x, int y) throws InvalidMoveException {
        var user = crudUserService.getUser(userId);
        var game = crudGameService.getGame(gameId);
        game.makeAmove(user, gameSymbol, x, y);
    }

    @Override
    public boolean startGame(String gameId) {
        var game = crudGameService.getGame(gameId);
        if (!game.bothPlayersJoined()) {
            return false;
        }
        try {
            game.start();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
