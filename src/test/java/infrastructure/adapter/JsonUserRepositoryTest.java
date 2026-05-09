package infrastructure.adapter;

import domain.User;
import infrastructure.adapter.out.JsonUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonUserRepositoryTest {

    private JsonUserRepository repository;
    private File tempFile;

    @BeforeEach
    void setUp() throws Exception {
        repository = new JsonUserRepository();
        tempFile = File.createTempFile("users_test", ".json");
        tempFile.delete(); // Delete so it doesn't exist as 0 bytes, avoiding JSON parse error
        
        // Use reflection to change the 'file' field so we don't overwrite the real 'users.json'
        Field fileField = JsonUserRepository.class.getDeclaredField("file");
        fileField.setAccessible(true);
        fileField.set(repository, tempFile);
    }

    @AfterEach
    void tearDown() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void testSaveAndGet() {
        // Arrange
        User user = new User("user123", "Alice");

        // Act
        repository.save(user);
        Optional<User> retrievedUser = repository.get("user123");

        // Assert
        assertTrue(retrievedUser.isPresent());
        assertEquals("user123", retrievedUser.get().id());
        assertEquals("Alice", retrievedUser.get().name());
    }

    @Test
    void testGetNonExistentUser() {
        // Act
        Optional<User> retrievedUser = repository.get("nonexistent");

        // Assert
        assertTrue(retrievedUser.isEmpty());
    }

    @Test
    void testUpdateExistingUser() {
        // Arrange
        User user1 = new User("user1", "Bob");
        User user1Updated = new User("user1", "Bob Updated");

        // Act
        repository.save(user1);
        repository.save(user1Updated);
        Optional<User> retrievedUser = repository.get("user1");

        // Assert
        assertTrue(retrievedUser.isPresent());
        assertEquals("Bob Updated", retrievedUser.get().name());
    }
}
