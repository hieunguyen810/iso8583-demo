package com.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

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
        
        SpringApplication.run(DemoApplication.class, args);
    }
}