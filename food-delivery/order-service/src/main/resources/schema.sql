CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      VARCHAR(64)  NOT NULL,
    restaurant_id    BIGINT       NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    total_paise      BIGINT       NOT NULL CHECK (total_paise >= 0),
    delivery_address VARCHAR(255) NOT NULL,
    idempotency_key  VARCHAR(100) UNIQUE,
    payment_id       VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders (customer_id, created_at DESC);

CREATE TABLE IF NOT EXISTS order_lines (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    menu_item_id     BIGINT       NOT NULL,
    name             VARCHAR(120) NOT NULL,
    quantity         INT          NOT NULL CHECK (quantity > 0),
    unit_price_paise BIGINT       NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_order_lines_order ON order_lines (order_id);
