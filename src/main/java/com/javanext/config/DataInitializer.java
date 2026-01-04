package com.javanext.config;

import com.javanext.inventory.domain.Product;
import com.javanext.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(ProductRepository productRepository) {
        return args -> {
            // Check if data already exists
            if (productRepository.count() > 0) {
                logger.info("Sample data already exists. Skipping initialization.");
                return;
            }

            logger.info("Initializing sample product data...");

            // Create sample products
            Product laptop = new Product();
            laptop.setSku("LAPTOP-001");
            laptop.setName("Gaming Laptop");
            laptop.setQuantity(100);
            laptop.setReservedQuantity(0);
            laptop.setPrice(new BigDecimal("1299.99"));
            productRepository.save(laptop);

            Product mouse = new Product();
            mouse.setSku("MOUSE-001");
            mouse.setName("Wireless Mouse");
            mouse.setQuantity(500);
            mouse.setReservedQuantity(0);
            mouse.setPrice(new BigDecimal("29.99"));
            productRepository.save(mouse);

            Product keyboard = new Product();
            keyboard.setSku("KEYBOARD-001");
            keyboard.setName("Mechanical Keyboard");
            keyboard.setQuantity(250);
            keyboard.setReservedQuantity(0);
            keyboard.setPrice(new BigDecimal("89.99"));
            productRepository.save(keyboard);

            Product monitor = new Product();
            monitor.setSku("MONITOR-001");
            monitor.setName("4K Monitor");
            monitor.setQuantity(75);
            monitor.setReservedQuantity(0);
            monitor.setPrice(new BigDecimal("499.99"));
            productRepository.save(monitor);

            Product headset = new Product();
            headset.setSku("HEADSET-001");
            headset.setName("Gaming Headset");
            headset.setQuantity(200);
            headset.setReservedQuantity(0);
            headset.setPrice(new BigDecimal("79.99"));
            productRepository.save(headset);

            logger.info("Sample data initialization complete. {} products created.", productRepository.count());
        };
    }
}
