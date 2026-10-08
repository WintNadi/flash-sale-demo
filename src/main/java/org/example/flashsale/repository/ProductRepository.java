package org.example.flashsale.repository;

import java.sql.*;

public class ProductRepository {

    /** Insert a product and return its generated ID. */
    public long insert(String name, double regularPrice) {
        String sql = "INSERT INTO products (name, regular_price) VALUES (?, ?) RETURNING id";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setBigDecimal(2, java.math.BigDecimal.valueOf(regularPrice));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert product", e);
        }
    }
}
