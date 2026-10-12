package org.example.flashsale.repository;

import org.example.flashsale.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert a new user. Returns the generated ID. */
    public long insert(String email, String passwordHash) {
        String sql = "INSERT INTO users (email, password_hash) VALUES (?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated user ID");
        return key.longValue();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, created_at FROM users WHERE email = ?";
        List<User> results = jdbc.query(sql, ROW_MAPPER, email);
        return results.stream().findFirst();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, email, password_hash, created_at FROM users WHERE id = ?";
        List<User> results = jdbc.query(sql, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

}
