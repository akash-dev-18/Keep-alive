package akakash.backend.user;

import akakash.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService self;

    @Test
    void createUserAssignsFreePlanAndPersistsTimestamps() {
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = new UserService(userRepository, null)
                .createUser("clerk_123", "user@example.com", "User");

        assertThat(user.getClerkUserId()).isEqualTo("clerk_123");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPlan()).isEqualTo(Plan.FREE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void getOrCreateUserReturnsExistingUserWithoutCreating() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .clerkUserId("clerk_123")
                .email("user@example.com")
                .plan(Plan.PRO)
                .build();
        when(userRepository.findByClerkUserId("clerk_123")).thenReturn(Optional.of(existing));

        User user = new UserService(userRepository, self)
                .getOrCreateUser("clerk_123", "new@example.com", "New");

        assertThat(user).isSameAs(existing);
    }

    @Test
    void getOrCreateUserRecoversFromConcurrentCreateConflict() {
        User createdByOtherRequest = User.builder()
                .id(UUID.randomUUID())
                .clerkUserId("clerk_123")
                .email("user@example.com")
                .plan(Plan.FREE)
                .build();
        when(userRepository.findByClerkUserId("clerk_123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdByOtherRequest));
        when(self.createUser("clerk_123", "user@example.com", "User"))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        User user = new UserService(userRepository, self)
                .getOrCreateUser("clerk_123", "user@example.com", "User");

        assertThat(user).isSameAs(createdByOtherRequest);
    }

    @Test
    void updateUserChangesEmailNameAndTimestamp() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .clerkUserId("clerk_123")
                .email("old@example.com")
                .name("Old")
                .plan(Plan.FREE)
                .build();
        when(userRepository.findByClerkUserId("clerk_123")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = new UserService(userRepository, self)
                .updateUser("clerk_123", "new@example.com", "New");

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UserService(userRepository, self).findById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
