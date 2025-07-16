package com.myecom.repository;

import com.myecom.model.Order;
import com.myecom.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test per UserRepository - verifica query e relazioni
 */
class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Mario");
        assertThat(found.get().getLastName()).isEqualTo("Rossi");
    }

    @Test
    void shouldReturnEmptyWhenEmailNotExists() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckIfEmailExists() {
        // When
        boolean exists = userRepository.existsByEmail("test@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldFindUsersByRole() {
        // When
        List<User> users = userRepository.findByRole(User.Role.USER);
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("test@example.com");

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void shouldFindEnabledUsers() {
        // Given - crea un utente disabilitato
        User disabledUser = User.builder()
                .email("disabled@example.com")
                .password("password")
                .firstName("Disabled")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(false)
                .build();
        entityManager.persistAndFlush(disabledUser);

        // When
        List<User> enabledUsers = userRepository.findByEnabled(true);
        List<User> disabledUsers = userRepository.findByEnabled(false);

        // Then
        assertThat(enabledUsers).hasSize(2); // testUser + adminUser
        assertThat(disabledUsers).hasSize(1);
        assertThat(disabledUsers.get(0).getEmail()).isEqualTo("disabled@example.com");
    }

    @Test
    void shouldFindUsersByCity() {
        // When
        List<User> milanUsers = userRepository.findByCity("Milano");
        List<User> romeUsers = userRepository.findByCity("Roma");

        // Then
        assertThat(milanUsers).hasSize(1);
        assertThat(milanUsers.get(0).getEmail()).isEqualTo("test@example.com");
        assertThat(romeUsers).isEmpty();
    }

    @Test
    void shouldFindUsersCreatedInPeriod() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // When
        List<User> users = userRepository.findByCreatedAtBetween(start, end);

        // Then
        assertThat(users).hasSize(2); // testUser + adminUser
    }

    @Test
    void shouldCountActiveUsers() {
        // When
        long activeCount = userRepository.countActiveUsers();

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    void shouldFindActiveAdmins() {
        // When
        List<User> activeAdmins = userRepository.findActiveAdmins();

        // Then
        assertThat(activeAdmins).hasSize(1);
        assertThat(activeAdmins.get(0).getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(activeAdmins.get(0).isEnabled()).isTrue();
    }

    @Test
    void shouldSearchUsersByName() {
        // When - ricerca case insensitive
        List<User> foundByFirstName = userRepository.searchUsersByName("mario");
        List<User> foundByLastName = userRepository.searchUsersByName("ROSSI");
        List<User> foundPartial = userRepository.searchUsersByName("ar"); // trova "Mario"

        // Then
        assertThat(foundByFirstName).hasSize(1);
        assertThat(foundByFirstName.get(0).getEmail()).isEqualTo("test@example.com");

        assertThat(foundByLastName).hasSize(1);
        assertThat(foundByLastName.get(0).getEmail()).isEqualTo("test@example.com");

        assertThat(foundPartial).hasSize(1);
        assertThat(foundPartial.get(0).getFirstName()).isEqualTo("Mario");
    }

    @Test
    void shouldFindUsersWithOrdersSince() {
        // Given - crea un ordine per testUser
        var order = Order.builder()
                .orderNumber("ORD-001")
                .user(testUser)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Via Roma 1")
                .createdAt(LocalDateTime.now()) // Esplicito per il test
                .build();

        entityManager.persistAndFlush(order);

        // When
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        List<User> usersWithOrders = userRepository.findUsersWithOrdersSince(since);

        // Then
        assertThat(usersWithOrders).hasSize(1);
        assertThat(usersWithOrders.get(0).getEmail()).isEqualTo("test@example.com");
    }
}
