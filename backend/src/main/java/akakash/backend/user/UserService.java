package akakash.backend.user;

import akakash.backend.common.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserService self;

    public UserService(UserRepository userRepository, @Lazy UserService self) {
        this.userRepository = userRepository;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public User findByClerkUserId(String clerkUserId) {
        return userRepository.findByClerkUserId(clerkUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for clerk ID: " + clerkUserId));
    }

    public User getOrCreateUser(String clerkUserId, String email, String name) {
        return userRepository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> {
                    try {
                        return self.createUser(clerkUserId, email, name);
                    } catch (DataIntegrityViolationException ex) {
                        return userRepository.findByClerkUserId(clerkUserId)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createUser(String clerkUserId, String email, String name) {
        User user = User.builder()
                .clerkUserId(clerkUserId)
                .email(email)
                .name(name)
                .plan(Plan.FREE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public User updateUser(String clerkUserId, String email, String name) {
        User user = findByClerkUserId(clerkUserId);
        user.setEmail(email);
        user.setName(name);
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String clerkUserId) {
        User user = findByClerkUserId(clerkUserId);
        userRepository.delete(user);
    }
}