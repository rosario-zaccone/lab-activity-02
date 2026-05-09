package domain.port.in;

import domain.User;

public interface UserUseCase {
    User registerUser(User user);
}
