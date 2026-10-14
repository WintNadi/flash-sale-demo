package org.example.flashsale.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert without image — used by tests. */
    public long insert(String name, double regularPrice) {
        return insert(name, regularPrice, null);
    }

    /** Insert with an optional image URL. */
    public long insert(String name, double regularPrice, String imageUrl) {
        String sql = "INSERT INTO products (name, regular_price, image_url) VALUES (?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, name);
            ps.setBigDecimal(2, BigDecimal.valueOf(regularPrice));
            ps.setString(3, imageUrl);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated product ID");
        return key.longValue();
    }
}