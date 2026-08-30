package com.starbuck.moneytracker.testsupport;

import java.util.List;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Egy eldobható MySQL konténert indít. Egyszer indul a teljes futás alatt. 
@Testcontainers
@ActiveProfiles("test")
public abstract class MySqlContainerTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.30")
            .withDatabaseName("testing")
            .withUsername("root")
            .withPassword("root");

    static {
        // Fix port, hogy a konténer futása alatt MySQL Workbench-ből is elérhető legyen.
        MYSQL.setPortBindings(List.of("3307:3306"));
    }
}
