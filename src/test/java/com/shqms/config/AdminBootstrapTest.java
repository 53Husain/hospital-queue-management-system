package com.shqms.config;

import com.shqms.entity.User;
import com.shqms.enums.UserRole;
import com.shqms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test void createsFirstAdminWithHashedPassword() {
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        new AdminBootstrap(users, encoder, "admin@example.com", "unique-password-123").run(null);
        var capture = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(capture.capture());
        assertThat(capture.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(encoder.matches("unique-password-123", capture.getValue().getPassword())).isTrue();
    }
    @Test void existingAdminIsNeverOverwritten() {
        when(users.existsByRole(UserRole.ADMIN)).thenReturn(true);
        new AdminBootstrap(users, encoder, "admin@example.com", "unique-password-123").run(null);
        verify(users, never()).save(any());
    }
    @Test void existingPatientCannotBePromotedByBootstrap() {
        when(users.findByEmail("patient@example.com")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> new AdminBootstrap(users, encoder, "patient@example.com", "unique-password-123").run(null))
            .isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }
    @Test void invalidConfigurationFailsWithoutSaving() {
        for (String[] values : new String[][]{{"bad email", "unique-password-123"}, {"a@b.com", "short"}, {"a@b.com", "é".repeat(40)}}) {
            assertThatThrownBy(() -> new AdminBootstrap(users, encoder, values[0], values[1]).run(null))
                .isInstanceOf(IllegalStateException.class);
        }
        verify(users, never()).save(any());
    }
}
