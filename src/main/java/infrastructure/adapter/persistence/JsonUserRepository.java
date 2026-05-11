package infrastructure.adapter.persistence;

import application.port.out.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.User;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonUserRepository implements UserRepository {

    private static final String FILE_PATH = "users.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File file = new File(FILE_PATH);

    @Override
    public User save(User user) {
        try {
            List<UserEntity> entities = file.exists()
                    ? objectMapper.readValue(file, new TypeReference<List<UserEntity>>() {})
                    : new ArrayList<>();

            entities.removeIf(e -> e.getId().equals(user.id()));
            entities.add(toEntity(user));

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, entities);

            return user;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    @Override
    public Optional<User> get(String id) {
        try {
            if (!file.exists()) return Optional.empty();

            List<UserEntity> entities = objectMapper.readValue(file, new TypeReference<List<UserEntity>>() {});
            return entities.stream()
                    .filter(e -> e.getId().equals(id))
                    .findFirst()
                    .map(this::toDomain);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user", e);
        }
    }

    @Override
    public List<User> getAll() {
        try {
            if (!file.exists()) return new ArrayList<>();

            List<UserEntity> entities = objectMapper.readValue(file, new TypeReference<List<UserEntity>>() {});
            return entities.stream()
                    .map(this::toDomain)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get all users", e);
        }
    }

    private UserEntity toEntity(User user) {
        return new UserEntity(user.id(), user.name());
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName());
    }
}