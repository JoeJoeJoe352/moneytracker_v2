package com.starbuck.moneytracker.testsupport;

import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * A migrációk (pl. V20260822__add_predefined_categories_hu_lang.sql) által
 * beszúrt előre definiált kategóriákat törli, közvetlenül a Flyway migráció
 * lefutása után, még mielőtt bármelyik teszt elindulna. Enélkül a tesztek
 * a migrációban lévő kategóriák számától függenének, ami minden új
 * előre definiált kategóriát hozzáadó migrációnál törne.
 */
@Component
@Profile("test")
public class PredefinedCategoryCleanupFlywayCallback implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("DELETE FROM category");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clean up predefined categories after migration", e);
        }
    }

    @Override
    public String getCallbackName() {
        return "PredefinedCategoryCleanupFlywayCallback";
    }
}
