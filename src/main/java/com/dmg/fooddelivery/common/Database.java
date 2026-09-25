package com.dmg.fooddelivery.common;

import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Component
public class Database {
    private final JdbcTemplate jdbc;

    public Database(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(String sql, Object... args) {
        var holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            return statement;
        }, holder);
        return java.util.Objects.requireNonNull(holder.getKey()).longValue();
    }
}
