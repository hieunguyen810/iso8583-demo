package com.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        // Check command line arguments
        if (args.length > 0) {
            String mode = args[0].toLowerCase();
            
            if ("server".equals(mode)) {
                System.setProperty("app.mode", "server");
                System.out.println("🚀 Starting in SERVER mode only...");
            } else if ("client".equals(mode)) {
                System.setProperty("app.mode", "client");
                System.out.println("🔄 Starting in CLIENT mode only...");
            } else {
                System.out.println("❓ Usage: java -jar demo.jar [server|client]");
                System.out.println("📝 Running in BOTH mode (default)...");
                System.setProperty("app.mode", "both");
            }
        } else {
            System.setProperty("app.mode", "both");
            System.out.println("📝 Running in BOTH mode (default)...");
        }
        
        // Check if database writes should be disabled via environment or system property
        String dbWriteEnabled = System.getProperty("iso8583.database.write.enabled", 
            System.getenv().getOrDefault("ISO8583_DATABASE_WRITE_ENABLED", "true"));
        
        if ("false".equals(dbWriteEnabled)) {
            System.out.println("🚫 Database writes disabled - starting without JPA");
            SpringApplication.run(DemoApplicationNoDb.class, args);
        } else {
            System.out.println("💾 Database writes enabled - starting with JPA");
            SpringApplication.run(DemoApplication.class, args);
        }
    }
}