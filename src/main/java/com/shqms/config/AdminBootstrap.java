package com.shqms.config;

import com.shqms.entity.User;
import com.shqms.enums.UserRole;
import com.shqms.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users; this.encoder = encoder; this.email = email; this.password = password;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(UserRole.ADMIN)) return;
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set a valid bootstrap admin email and a password of at least 12 characters, at most 72 UTF-8 bytes");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Bootstrap email already belongs to an account; refusing to change its role");
        }
        User user = new User();
        user.setEmail(email); user.setFullName("Administrator");
        user.setPassword(encoder.encode(password)); user.setRole(UserRole.ADMIN);
        users.save(user);
    }
}
