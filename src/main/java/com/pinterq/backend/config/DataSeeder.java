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
            // Selalu pastikan pinterq_admin ada
            if (userRepository.findByUsername("pinterq_admin").isEmpty()) {
                User admin = User.builder()
                        .username("pinterq_admin")
                        .email("admin@pinterq.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role(Role.SUPERADMIN)
                        .approvalStatus(ApprovalStatus.APPROVED)
                        .build();
                userRepository.save(admin);
                System.out.println("ADMIN CREATED: pinterq_admin / password123");
                
                // Buat kategori default jika belum ada
                if (categoryRepository.findAll().isEmpty()) {
                    Category category = Category.builder()
                            .name("Umum")
                            .user(admin)
                            .build();
                    categoryRepository.save(category);
                }
            } else {
                System.out.println("ADMIN ALREADY EXISTS: pinterq_admin");
            }
            
            // Buat test_user jika belum ada
            if (userRepository.findByUsername("test_user").isEmpty()) {
                User testUser = User.builder()
                        .username("test_user")
                        .email("test@pinterq.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role(Role.USER)
                        .approvalStatus(ApprovalStatus.APPROVED)
                        .build();
                userRepository.save(testUser);
                System.out.println("TEST USER CREATED: test_user / password123");
            }

            System.out.println("========== DATA SEEDING CHECK COMPLETED ==========");
        };
    }
}
