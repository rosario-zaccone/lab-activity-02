package application.service;

import application.UserNotFoundException;
import application.port.in.GetUserUseCase;
import application.port.in.RegisterUserUseCase;
import application.port.out.UserRepository;
import domain.User;


public class CrudUserService implements RegisterUserUseCase, GetUserUseCase {
    private final UserRepository repository;

    public CrudUserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User registerUser(User user) {
        int nextId = repository.getAll().stream()
                .mapToInt(u -> Integer.parseInt(u.id().replaceAll("[^0-9]", "")))
                .max()
                .orElse(0) + 1;
        return repository.save(new User("user-" + nextId, user.name()));
    }

    @Override
    public User getUser(String id) {
        var user = repository.get(id);
        if (user.isEmpty())
            throw new UserNotFoundException(id);
        else
            return user.get();
    }
}