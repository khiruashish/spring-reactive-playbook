CREATE TABLE IF NOT EXISTS restaurants (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(120) NOT NULL,
    cuisine  VARCHAR(60)  NOT NULL,
    address  VARCHAR(255) NOT NULL,
    open     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS menu_items (
    id            BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT       NOT NULL REFERENCES restaurants (id),
    name          VARCHAR(120) NOT NULL,
    description   VARCHAR(500),
    price_paise   BIGINT       NOT NULL CHECK (price_paise > 0),
    available     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant ON menu_items (restaurant_id);
