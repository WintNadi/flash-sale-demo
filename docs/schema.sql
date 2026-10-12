CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          regular_price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE flash_sales (
                             id BIGSERIAL PRIMARY KEY,
                             product_id BIGINT NOT NULL REFERENCES products(id),
                             total_stock INT NOT NULL,
                             remaining_stock INT NOT NULL,
                             sale_price NUMERIC(10, 2) NOT NULL,
                             starts_at TIMESTAMP NOT NULL,
                             ends_at TIMESTAMP NOT NULL,
                             status VARCHAR(20) NOT NULL,
                             version INT NOT NULL DEFAULT 0,
                             CHECK (remaining_stock >= 0)
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL REFERENCES users(id),
                        flash_sale_id BIGINT NOT NULL REFERENCES flash_sales(id),
                        quantity INT NOT NULL,
                        total_price NUMERIC(10, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flash_sales_status ON flash_sales(status);
CREATE INDEX idx_orders_user ON orders(user_id);