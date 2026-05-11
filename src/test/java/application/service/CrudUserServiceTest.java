package application.service;

import application.UserNotFoundException;
import application.port.out.UserRepository;
import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrudUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CrudUserService crudUserService;

    @Test
    void testRegisterUser() {
        // Arrange
        User user = new User("user-1", "John Doe");
        when(userRepository.getAll()).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = crudUserService.registerUser(user);

        // Assert
        verify(userRepository).save(new User("user-1", "John Doe"));
        assertEquals("John Doe", result.name());
    }

    @Test
    void testGetUser_returnsUser_whenExists() {
        // Arrange
        User user = new User("user-1", "John Doe");
        when(userRepository.get("user-1")).thenReturn(Optional.of(user));

        // Act
        User result = crudUserService.getUser("user-1");

        // Assert
        assertEquals(user, result);
    }

    @Test
    void testGetUser_throwsUserNotFoundException_whenNotExists() {
        // Arrange
        when(userRepository.get("99")).thenReturn(Optional.empty());

        // Assert
        assertThrows(UserNotFoundException.class, () -> crudUserService.getUser("99"));
    }
}