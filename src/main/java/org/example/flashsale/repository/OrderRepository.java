package org.example.flashsale.repository;

import org.example.flashsale.model.Order;
import org.example.flashsale.model.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long userId, long flashSaleId, int quantity, double totalPrice) {
        String sql = """
            INSERT INTO orders (user_id, flash_sale_id, quantity, total_price, status, created_at)
            VALUES (?, ?, ?, ?, 'RESERVED', NOW())
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, userId);
            ps.setLong(2, flashSaleId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, BigDecimal.valueOf(totalPrice));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated order ID");
        return key.longValue();
    }

    public Optional<Order> findById(long id) {
        String sql = """
        SELECT id, user_id, flash_sale_id, quantity, total_price, status, created_at
        FROM orders
        WHERE id = ?
        """;
        List<Order> results = jdbc.query(sql, (rs, rowNum) -> new Order(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("flash_sale_id"),
                rs.getInt("quantity"),
                rs.getDouble("total_price"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                OrderStatus.valueOf(rs.getString("status"))   // ← ADDED
        ), id);
        return results.stream().findFirst();
    }

    public List<Order> findByUserId(long userId) {
        String sql = """
        SELECT id, user_id, flash_sale_id, quantity, total_price, status, created_at
        FROM orders
        WHERE user_id = ?
        ORDER BY created_at DESC
        """;
        return jdbc.query(sql, (rs, rowNum) -> new Order(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("flash_sale_id"),
                rs.getInt("quantity"),
                rs.getDouble("total_price"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                OrderStatus.valueOf(rs.getString("status"))   // ← ADDED
        ), userId);
    }

    public void updateStatus(long orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        jdbc.update(sql, status, orderId);
    }

    /** Fetch orders belonging to a specific user (for authorization checks). */
    public boolean belongsToUser(long orderId, long userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE id = ? AND user_id = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, orderId, userId);
        return count != null && count > 0;
    }

    public List<OrderWithProduct> findByUserIdWithProduct(long userId) {
        String sql = """
        SELECT o.id, o.user_id, o.flash_sale_id, o.quantity, o.total_price,
               o.status, o.created_at,
               p.name AS product_name
        FROM orders o
        JOIN flash_sales fs ON o.flash_sale_id = fs.id
        JOIN products p ON fs.product_id = p.id
        WHERE o.user_id = ?
        ORDER BY o.created_at DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> new OrderWithProduct(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("flash_sale_id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("total_price"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId);
    }

    public record OrderWithProduct(
            long id,
            long userId,
            long flashSaleId,
            String productName,
            int quantity,
            double totalPrice,
            String status,
            LocalDateTime createdAt
    ) {}
}