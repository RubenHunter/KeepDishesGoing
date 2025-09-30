-- Insert sample restaurants
INSERT INTO restaurants (id, name, status) VALUES
    ('a6a52c73-9070-4128-a988-255383b941bc', 'Test Bistro', 'ACTIVE'),
('b7b62d84-1234-5678-9101-112131415161', 'Pizza Palace', 'ACTIVE'),
('c8c73e95-2345-6789-1011-121314151617', 'Sushi World', 'INACTIVE');

-- Insert sample dishes
INSERT INTO dishes (id, restaurant_id, name, description, price, category, status) VALUES
    ('d1d82f06-3456-7891-0111-213141516171', 'a6a52c73-9070-4128-a988-255383b941bc', 'Margherita Pizza', 'Classic pizza with tomato and mozzarella', 10.00, 'MAIN_COURSE', 'PUBLISHED'),
('d2d93f17-4567-8910-1112-314151617181', 'a6a52c73-9070-4128-a988-255383b941bc', 'Tiramisu', 'Italian dessert', 6.00, 'DESSERT', 'OUT_OF_STOCK'),
('d3da4018-5678-9101-1121-415161718191', 'b7b62d84-1234-5678-9101-112131415161', 'Sushi Roll', 'Fresh salmon roll', 12.00, 'MAIN_COURSE', 'DRAFT');
