package application.port.in;

import domain.GameSymbolType;
import application.InvalidJoinException;

public interface JoinGameUseCase {
    void joinGame(String userId, String gameId, GameSymbolType gameSymbol) throws InvalidJoinException;
}
