CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE t_orders (
    id          UUID NOT NULL DEFAULT uuid_generate_v4(),
    order_number VARCHAR(255) DEFAULT NULL,
    sku_code    VARCHAR(255) DEFAULT NULL,
    price       DECIMAL(19, 2) NOT NULL,
    quantity    INT NOT NULL,
    PRIMARY KEY (id)
);