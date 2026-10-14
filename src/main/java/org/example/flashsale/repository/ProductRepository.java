package org.example.flashsale.repository;

import org.example.flashsale.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -------- Legacy methods (used by tests) --------

    /** Insert without image or seller — assigns to first admin. Used by tests. */
    public long insert(String name, double regularPrice) {
        return insert(name, regularPrice, null, null);
    }

    public long insert(String name, double regularPrice, String imageUrl) {
        return insert(name, regularPrice, imageUrl, null);
    }

    /** Full insert. If sellerId is null, assigns to the first admin. */
    public long insert(String name, double regularPrice, String imageUrl, Long sellerId) {
        String sql = """
            INSERT INTO products (name, regular_price, image_url, seller_id)
            VALUES (?, ?, ?, COALESCE(?, (SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1)))
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, name);
            ps.setBigDecimal(2, BigDecimal.valueOf(regularPrice));
            ps.setString(3, imageUrl);
            if (sellerId == null) ps.setNull(4, java.sql.Types.BIGINT);
            else ps.setLong(4, sellerId);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new RuntimeException("Failed to get generated product ID");
        return key.longValue();
    }

    // -------- Seller methods --------

    public Optional<Product> findById(long id) {
        String sql = "SELECT id, seller_id, name, regular_price, image_url FROM products WHERE id = ?";
        List<Product> results = jdbc.query(sql, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public List<Product> findBySellerId(long sellerId) {
        String sql = """
            SELECT id, seller_id, name, regular_price, image_url
            FROM products
            WHERE seller_id = ?
            ORDER BY id DESC
            """;
        return jdbc.query(sql, ROW_MAPPER, sellerId);
    }

    public void update(long id, String name, double regularPrice, String imageUrl) {
        String sql = "UPDATE products SET name = ?, regular_price = ?, image_url = ? WHERE id = ?";
        jdbc.update(sql, name, BigDecimal.valueOf(regularPrice), imageUrl, id);
    }

    public void deleteById(long id) {
        jdbc.update("DELETE FROM products WHERE id = ?", id);
    }

    /** Returns true if a product has any flash sales attached. */
    public boolean hasFlashSales(long productId) {
        String sql = "SELECT COUNT(*) FROM flash_sales WHERE product_id = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, productId);
        return count != null && count > 0;
    }

    private static final RowMapper<Product> ROW_MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getObject("seller_id", Long.class),
            rs.getString("name"),
            rs.getDouble("regular_price"),
            rs.getString("image_url")
    );
}