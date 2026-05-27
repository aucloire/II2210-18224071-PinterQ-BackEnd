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

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, CategoryRepository categoryRepository) {
        return args -> {
            System.out.println(">>> RUNNING DATA SEEDER...");
            
            // Force Create/Update pinterq_admin
            Optional<User> existingAdmin = userRepository.findByUsername("pinterq_admin");
            User admin;
            
            if (existingAdmin.isEmpty()) {
                admin = User.builder()
                        .username("pinterq_admin")
                        .email("admin@pinterq.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role(Role.SUPERADMIN)
                        .approvalStatus(ApprovalStatus.APPROVED)
                        .build();
                userRepository.save(admin);
                System.out.println("ADMIN CREATED: pinterq_admin / password123");
            } else {
                admin = existingAdmin.get();
                admin.setPasswordHash(passwordEncoder.encode("password123"));
                admin.setRole(Role.SUPERADMIN);
                admin.setApprovalStatus(ApprovalStatus.APPROVED);
                userRepository.save(admin);
                System.out.println("ADMIN PASSWORD RESET: pinterq_admin / password123");
            }

            // Ensure Default Category
            if (categoryRepository.findAll().isEmpty()) {
                Category category = Category.builder()
                        .name("Umum")
                        .user(admin)
                        .build();
                categoryRepository.save(category);
                System.out.println("DEFAULT CATEGORY CREATED");
            }

            System.out.println("========== DATA SEEDER COMPLETED ==========");
        };
    }
}
