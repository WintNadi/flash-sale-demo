package org.example.flashsale.repository;

import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.FlashSaleStatus;
import org.example.flashsale.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class FlashSaleRepository {

    private final JdbcTemplate jdbc;

    public FlashSaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * ⭐ THE ATOMIC RESERVE.
     */
    public boolean tryReserve(long flashSaleId, int quantity) {
        String sql = """
            UPDATE flash_sales
            SET remaining_stock = remaining_stock - ?,
                status = CASE
                    WHEN remaining_stock - ? = 0 THEN 'SOLD_OUT'
                    ELSE status
                END
            WHERE id = ? AND remaining_stock >= ?
            """;

        int rowsAffected = jdbc.update(sql, quantity, quantity, flashSaleId, quantity);
        return rowsAffected == 1;
    }

    /**
     * Return stock to a flash sale (used when a reservation expires or is cancelled).
     */
    public void release(long flashSaleId, int quantity) {
        String sql = """
            UPDATE flash_sales
            SET remaining_stock = remaining_stock + ?,
                status = CASE
                    WHEN status = 'SOLD_OUT' THEN 'ACTIVE'
                    ELSE status
                END
            WHERE id = ?
            """;
        jdbc.update(sql, quantity, flashSaleId);
    }

    public long insert(long productId, int totalStock, double salePrice,
                       LocalDateTime startsAt, LocalDateTime endsAt) {
        String sql = """
            INSERT INTO flash_sales
              (product_id, total_stock, remaining_stock, sale_price, starts_at, ends_at, status)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, productId);
            ps.setInt(2, totalStock);
            ps.setInt(3, totalStock);
            ps.setBigDecimal(4, BigDecimal.valueOf(salePrice));
            ps.setTimestamp(5, Timestamp.valueOf(startsAt));
            ps.setTimestamp(6, Timestamp.valueOf(endsAt));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated sale ID");
        return key.longValue();
    }

    public Optional<FlashSale> findById(long id) {
        String sql = """
            SELECT fs.id, fs.total_stock, fs.remaining_stock, fs.sale_price,
                   fs.starts_at, fs.ends_at, fs.status,
                   p.id AS p_id, p.seller_id AS p_seller_id, p.name AS p_name,
                   p.regular_price AS p_price, p.image_url AS p_image_url
            FROM flash_sales fs
            JOIN products p ON fs.product_id = p.id
            WHERE fs.id = ?
            """;

        List<FlashSale> results = jdbc.query(sql, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public List<FlashSale> findAll() {
        String sql = """
            SELECT fs.id, fs.total_stock, fs.remaining_stock, fs.sale_price,
                   fs.starts_at, fs.ends_at, fs.status,
                   p.id AS p_id, p.seller_id AS p_seller_id, p.name AS p_name,
                   p.regular_price AS p_price, p.image_url AS p_image_url
            FROM flash_sales fs
            JOIN products p ON fs.product_id = p.id
            ORDER BY fs.id
            """;
        return jdbc.query(sql, ROW_MAPPER);
    }

    public void deleteById(long id) {
        jdbc.update("DELETE FROM flash_sales WHERE id = ?", id);
    }

    private static final RowMapper<FlashSale> ROW_MAPPER = (rs, rowNum) -> {
        Product p = new Product(
                rs.getLong("p_id"),
                rs.getObject("p_seller_id", Long.class),
                rs.getString("p_name"),
                rs.getDouble("p_price"),
                rs.getString("p_image_url")
        );
        FlashSale fs = new FlashSale(
                rs.getLong("id"),
                p,
                rs.getInt("total_stock"),
                rs.getDouble("sale_price"),
                rs.getTimestamp("starts_at").toLocalDateTime(),
                rs.getTimestamp("ends_at").toLocalDateTime()
        );
        fs.setRemainingStockFromDb(rs.getInt("remaining_stock"));
        fs.setStatus(FlashSaleStatus.valueOf(rs.getString("status")));
        return fs;
    };
}