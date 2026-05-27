package com.pinterq.backend.config;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, CategoryRepository categoryRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("pinterq_admin")
                        .email("admin@pinterq.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role("SUPERADMIN")
                        .isApproved(true)
                        .build();
                userRepository.save(admin);

                Category category = Category.builder()
                        .name("Umum")
                        .user(admin)
                        .build();
                categoryRepository.save(category);

                User testUser = User.builder()
                        .username("test_user")
                        .email("test@pinterq.com")
                        .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rs5xE1gG1nL1m1cHy")
                        .role("USER")
                        .isApproved(true)
                        .build();
                userRepository.save(testUser);
                System.out.println("TEST USER DIBUAT: test_user / password123");

                System.out.println("========== DATA DUMMY BERHASIL DITAMBAHKAN ==========");
            }
        };
    }
}
