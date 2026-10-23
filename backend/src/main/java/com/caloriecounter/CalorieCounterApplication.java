package com.caloriecounter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class CalorieCounterApplication {

    public static void main(String[] args) {
        useLocalSocketDir();
        SpringApplication.run(CalorieCounterApplication.class, args);
    }

    /**
     * On some Windows machines Tomcat fails to start with
     * "Unable to establish loopback connection ... Invalid argument: connect",
     * because Java cannot create its internal socket file in the user's Temp folder.
     * Pointing Java at a folder inside the project avoids that.
     */
    private static void useLocalSocketDir() {
        if (System.getProperty("jdk.net.unixdomain.tmpdir") != null) return;
        try {
            Path dir = Path.of(System.getProperty("user.dir"), "target", "tmp");
            Files.createDirectories(dir);
            System.setProperty("jdk.net.unixdomain.tmpdir", dir.toAbsolutePath().toString());
        } catch (IOException | RuntimeException ignored) {
            // fall back to Java's default
        }
    }
}
