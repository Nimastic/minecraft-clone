package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AppTest {
    @Test 
    void appRunsWithoutException() {
        assertDoesNotThrow(() -> {
            App app = new App();
            app.run();
        }, "App should run without throwing exceptions");
    }
}
