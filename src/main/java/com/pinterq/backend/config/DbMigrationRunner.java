package com.pinterq.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbMigrationRunner implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Fix legacy 'USER' role to 'MURID'
            jdbc.execute("UPDATE users SET role = 'MURID' WHERE role = 'USER'");
            System.out.println("[DBMigration] Fixed legacy USER roles to MURID.");
        } catch (Exception e) {
            System.out.println("[DBMigration] Legacy role fix skipped (column/issue may not exist): " + e.getMessage());
        }

        try {
            jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_approved BOOLEAN DEFAULT TRUE");
        } catch (Exception e) { /* column already exists */ }

        System.out.println("[DBMigration] Schema migration completed.");
    }
}