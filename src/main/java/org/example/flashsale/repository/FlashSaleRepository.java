package org.example.flashsale.repository;

import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.FlashSaleStatus;
import org.example.flashsale.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlashSaleRepository {

    /**
     * ⭐ THE ATOMIC RESERVE.
     *
     * Returns true if stock was successfully reserved, false if sold out.
     * Thread-safe — the DB serializes concurrent updates for us.
     *
     * This single UPDATE is the entire concurrency solution.
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

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setLong(3, flashSaleId);
            ps.setInt(4, quantity);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected == 1;   // true only if we got the stock

        } catch (SQLException e) {
            throw new RuntimeException("Failed to reserve stock", e);
        }
    }

    /** Insert a flash sale and return its generated ID. */
    public long insert(long productId, int totalStock, double salePrice,
                       LocalDateTime startsAt, LocalDateTime endsAt) {
        String sql = """
            INSERT INTO flash_sales
              (product_id, total_stock, remaining_stock, sale_price, starts_at, ends_at, status)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
            RETURNING id
            """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, productId);
            ps.setInt(2, totalStock);
            ps.setInt(3, totalStock);
            ps.setBigDecimal(4, BigDecimal.valueOf(salePrice));
            ps.setTimestamp(5, Timestamp.valueOf(startsAt));
            ps.setTimestamp(6, Timestamp.valueOf(endsAt));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert flash sale", e);
        }
    }

    /** Find a flash sale by ID, joining product info. */
    public Optional<FlashSale> findById(long id) {
        String sql = """
            SELECT fs.id, fs.total_stock, fs.remaining_stock, fs.sale_price,
                   fs.starts_at, fs.ends_at, fs.status,
                   p.id AS p_id, p.name AS p_name, p.regular_price AS p_price
            FROM flash_sales fs
            JOIN products p ON fs.product_id = p.id
            WHERE fs.id = ?
            """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find sale " + id, e);
        }
    }

    /** List all flash sales. */
    public List<FlashSale> findAll() {
        String sql = """
            SELECT fs.id, fs.total_stock, fs.remaining_stock, fs.sale_price,
                   fs.starts_at, fs.ends_at, fs.status,
                   p.id AS p_id, p.name AS p_name, p.regular_price AS p_price
            FROM flash_sales fs
            JOIN products p ON fs.product_id = p.id
            ORDER BY fs.id
            """;

        List<FlashSale> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list sales", e);
        }
    }

    /** Delete a sale (useful for test cleanup). */
    public void deleteById(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM flash_sales WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete sale " + id, e);
        }
    }

    private FlashSale mapRow(ResultSet rs) throws SQLException {
        Product p = new Product(
                rs.getLong("p_id"),
                rs.getString("p_name"),
                rs.getDouble("p_price")
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
    }
}