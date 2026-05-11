package application.port.out;

import domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> get(String id);
    List<User> getAll();
}
