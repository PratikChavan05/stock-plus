-- StockPulse seed (Addendum A + sprint-2 placeholders)
INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) VALUES
  ('PRD-001', 'SKU-ELEC-001', 'Wireless Earbuds Pro',     'ELECTRONICS', 79.99,  45,  20, 3,  'ACTIVE',               42.00, 0.20, 'SUP-AUDIO'),
  ('PRD-002', 'SKU-ELEC-002', 'USB-C Hub 7-Port',           'ELECTRONICS', 34.99,  120, 30, 1,  'ACTIVE',               14.50, 0.18, 'SUP-CABLE'),
  ('PRD-003', 'SKU-APP-001',  'Organic Cotton T-Shirt',     'APPAREL',     24.99,  8,   15, 12, 'PRICE_REVIEW_PENDING', 9.20,  0.22, 'SUP-APPAREL'),
  ('PRD-004', 'SKU-APP-002',  'Running Shorts — Navy',      'APPAREL',     39.99,  55,  20, 2,  'ACTIVE',               16.00, 0.22, 'SUP-APPAREL'),
  ('PRD-005', 'SKU-HOME-001', 'Ceramic Pour-Over Set',      'HOME',        49.99,  22,  10, 4,  'ACTIVE',               21.00, 0.20, 'SUP-HOME'),
  ('PRD-006', 'SKU-HOME-002', 'LED Desk Lamp — Dimmable',   'HOME',        59.99,  0,   15, 0,  'OUT_OF_STOCK',         24.00, 0.18, 'SUP-HOME'),
  ('PRD-007', 'SKU-ELEC-003', 'Portable Charger 20K',       'ELECTRONICS', 44.99,  18,  25, 8,  'ACTIVE',               19.50, 0.20, 'SUP-POWER'),
  ('PRD-008', 'SKU-APP-003',  'Hoodie — Heather Grey',      'APPAREL',     54.99,  11,  12, 15, 'ACTIVE',               22.00, 0.22, 'SUP-APPAREL');
