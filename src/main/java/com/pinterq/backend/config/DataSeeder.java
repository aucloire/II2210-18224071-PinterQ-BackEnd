package com.pinterq.backend.config;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, CategoryRepository categoryRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("pinterq_admin")
                        .email("admin@pinterq.com")
                        .passwordHash("password123")
                        .build();
                userRepository.save(admin);

                Category category = Category.builder()
                        .name("Umum")
                        .user(admin)
                        .build();
                categoryRepository.save(category);
                
                System.out.println("========== DATA DUMMY BERHASIL DITAMBAHKAN ==========");
            }
        };
    }
}