package application.service;

import domain.port.in.UserUseCase;
import domain.User;
import domain.port.out.UserRepository;

public class UserService implements UserUseCase {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User registerUser(User user) {
        int nextId = repository.getAll().stream()
                .mapToInt(u -> Integer.parseInt(u.id()))
                .max()
                .orElse(0) + 1;

        return repository.save(new User(String.valueOf(nextId), user.name()));
    }
}