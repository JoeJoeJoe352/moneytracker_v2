package com.starbuck.moneytracker.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

// META-INF/spring.factories-ban van regisztrálva. A testing sémát törli ki teljesen, minden futtatáskor. 
// Ez még ApplicationContext létrehozása előtt fut le, flyway migrációk előtt. Csak így sikerült elérni, hogy minden futáskor friss db-ven induljon a testing
public class TestDatabaseResetListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String RESET_URL = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final AtomicBoolean RESET_DONE = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (!RESET_DONE.compareAndSet(false, true)) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(RESET_URL, "root", "root");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS testing");
            statement.execute("CREATE DATABASE testing");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset local test database", e);
        }
    }
}
