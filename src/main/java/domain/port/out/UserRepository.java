package domain.port;

import domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> get(String id);
    List<User> getAll();
}
