package com.pinterq.backend.config;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.User;
import com.pinterq.backend.model.User.ApprovalStatus;
import com.pinterq.backend.model.User.Role;
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
                        .role(Role.SUPERADMIN)
                        .approvalStatus(ApprovalStatus.APPROVED)
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
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role(Role.USER)
                        .approvalStatus(ApprovalStatus.APPROVED)
                        .build();
                userRepository.save(testUser);
                System.out.println("TEST USER DIBUAT: test_user / password123");

                System.out.println("========== DATA DUMMY BERHASIL DITAMBAHKAN ==========");
            }
        };
    }
}
