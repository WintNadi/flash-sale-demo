package org.example.flashsale.repository;

import org.example.flashsale.model.ApplicationStatus;
import org.example.flashsale.model.SellerApplication;
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
public class SellerApplicationRepository {

    private final JdbcTemplate jdbc;

    public SellerApplicationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(SellerApplication app) {
        String sql = """
            INSERT INTO seller_applications (user_id, business_name, description, status, created_at)
            VALUES (?, ?, ?, 'PENDING', NOW())
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, app.getUserId());
            ps.setString(2, app.getBusinessName());
            ps.setString(3, app.getDescription());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated application ID");
        return key.longValue();
    }

    public Optional<SellerApplication> findById(long id) {
        String sql = "SELECT * FROM seller_applications WHERE id = ?";
        List<SellerApplication> results = jdbc.query(sql, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public List<SellerApplication> findByUserId(long userId) {
        String sql = """
            SELECT * FROM seller_applications
            WHERE user_id = ?
            ORDER BY created_at DESC
            """;
        return jdbc.query(sql, ROW_MAPPER, userId);
    }

    /** Return the user's current pending application, if any. */
    public Optional<SellerApplication> findPendingByUserId(long userId) {
        String sql = """
            SELECT * FROM seller_applications
            WHERE user_id = ? AND status = 'PENDING'
            ORDER BY created_at DESC
            LIMIT 1
            """;
        List<SellerApplication> results = jdbc.query(sql, ROW_MAPPER, userId);
        return results.stream().findFirst();
    }

    /** List applications, optionally filtered by status. */
    public List<SellerApplication> findAll(ApplicationStatus statusFilter) {
        if (statusFilter == null) {
            String sql = "SELECT * FROM seller_applications ORDER BY created_at DESC";
            return jdbc.query(sql, ROW_MAPPER);
        }
        String sql = """
            SELECT * FROM seller_applications
            WHERE status = ?
            ORDER BY created_at DESC
            """;
        return jdbc.query(sql, ROW_MAPPER, statusFilter.name());
    }

    public void update(SellerApplication app) {
        String sql = """
            UPDATE seller_applications
            SET status = ?, reviewed_by = ?, reviewed_at = ?, rejection_reason = ?
            WHERE id = ?
            """;

        jdbc.update(sql,
                app.getStatus().name(),
                app.getReviewedBy(),
                app.getReviewedAt() == null ? null : Timestamp.valueOf(app.getReviewedAt()),
                app.getRejectionReason(),
                app.getId()
        );
    }

    private static final RowMapper<SellerApplication> ROW_MAPPER = (rs, rowNum) -> new SellerApplication(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("business_name"),
            rs.getString("description"),
            ApplicationStatus.valueOf(rs.getString("status")),
            rs.getObject("reviewed_by", Long.class),
            rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toLocalDateTime(),
            rs.getString("rejection_reason"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );
}
