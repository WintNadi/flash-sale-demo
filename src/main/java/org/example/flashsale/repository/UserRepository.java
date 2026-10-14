package org.example.flashsale.repository;

import org.example.flashsale.model.Role;
import org.example.flashsale.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Insert a new user with the BUYER role by default.
     * Role can be changed later by an admin.
     */
    public long insert(String email, String passwordHash) {
        String sql = "INSERT INTO users (email, password_hash, role) VALUES (?, ?, 'BUYER')";

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

    /**
     * Insert a user with a specific role.
     * Used by admin actions or tests.
     */
    public long insert(String email, String passwordHash, Role role) {
        String sql = "INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated user ID");
        return key.longValue();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, role, created_at FROM users WHERE email = ?";
        List<User> results = jdbc.query(sql, ROW_MAPPER, email);
        return results.stream().findFirst();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, email, password_hash, role, created_at FROM users WHERE id = ?";
        List<User> results = jdbc.query(sql, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    /**
     * Update a user's role. Used by admin promotion actions and by seller application approval.
     */
    public void updateRole(long userId, Role role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        jdbc.update(sql, role.name(), userId);
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getTimestamp("created_at").toLocalDateTime()
    );
}