package com.starbuck.moneytracker.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

//Egy eldobható MySQL konténert indít. Egyszer indul a teljes futás alatt.
@ActiveProfiles("test")
public abstract class MySqlContainerTest {

    // Szándékosan nincs lezárva a resource, a Ryuk resource handler fogja lezárni, amikor a JVM lezáródik
    @SuppressWarnings("resource")
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.30")
            .withDatabaseName("testing")
            .withUsername("root")
            .withPassword("root")
            .withReuse(true);

    static {
        // Fix port, hogy a konténer futása alatt MySQL Workbench-ből is elérhető
        // legyen.
        MYSQL.setPortBindings(List.of("3307:3306"));
        MYSQL.start();
        // withReuse(true) miatt a konténer futásokon átívelően megmarad, ezért indításkor
        // mindig üresre töröljük - így a sebességnövekedést megtartjuk anélkül, hogy az
        // előző futásból maradt piszkos állapotot örökölnénk.
        resetDatabase();
    }

    private static void resetDatabase() {
        String rootUrl = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection connection = DriverManager.getConnection(rootUrl, MYSQL.getUsername(), MYSQL.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS testing");
            statement.execute("CREATE DATABASE testing");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset reused test database", e);
        }
    }
}
