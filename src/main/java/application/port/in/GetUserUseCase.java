package application.port.in;

import domain.User;

import java.util.Optional;

public interface GetUserUseCase {
    User getUser(String id);
}
