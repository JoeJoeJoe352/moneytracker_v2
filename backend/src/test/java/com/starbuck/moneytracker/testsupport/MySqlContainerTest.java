package com.starbuck.moneytracker.testsupport;

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
            .withPassword("root");

    static {
        // Fix port, hogy a konténer futása alatt MySQL Workbench-ből is elérhető
        // legyen.
        MYSQL.setPortBindings(List.of("3307:3306"));
        MYSQL.start();
    }
}
