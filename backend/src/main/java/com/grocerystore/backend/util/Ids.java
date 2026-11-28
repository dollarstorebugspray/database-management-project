package com.grocerystore.backend.util;

import org.springframework.jdbc.core.JdbcTemplate;

public final class Ids {

    private Ids() {
    }

    public static int next(JdbcTemplate jdbcTemplate, String table, String column) {
        Integer next = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table + " FOR UPDATE",
                Integer.class);
        return next == null ? 1 : next;
    }
}
