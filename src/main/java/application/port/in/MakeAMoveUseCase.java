package application.port.in;

import domain.GameSymbolType;
import io.vertx.core.json.JsonObject;
import ttt_backend.InvalidMoveException;

public interface MakeAMoveUseCase {
    void makeAMove(String userId, String gameId, GameSymbolType gameSymbol, int x, int y) throws InvalidMoveException;
}
