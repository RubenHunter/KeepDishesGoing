-- Insert sample restaurants
INSERT INTO restaurants (id, name, status, owner_id, full_address, email, opening_hours, logo_url) VALUES
    ('a6a52c73-9070-4128-a988-255383b941bc', 'Test Bistro', 'ACTIVE', '11111111-1111-1111-1111-111111111111', 'Main Street 1, 1000 Brussels, BE', 'bistro@example.com', 'Mon-Sun 10:00-22:00', 'https://example.com/logo1.png'),
('b7b62d84-1234-5678-9101-112131415161', 'Pizza Palace', 'ACTIVE', '22222222-2222-2222-2222-222222222222', 'Second Ave 5, 2000 Antwerp, BE', 'pizza@example.com', 'Mon-Sun 11:00-23:00', 'https://example.com/logo2.png'),
('c8c73e95-2345-6789-1011-121314151617', 'Sushi World', 'INACTIVE', '33333333-3333-3333-3333-333333333333', 'Harbor Rd 10, 9000 Ghent, BE', 'sushi@example.com', 'Tue-Sun 12:00-21:00', 'https://example.com/logo3.png');

-- Insert sample dishes
INSERT INTO dishes (id, restaurant_id, name, description, price, category, status) VALUES
    ('d1d82f06-3456-7891-0111-213141516171', 'a6a52c73-9070-4128-a988-255383b941bc', 'Margherita Pizza', 'Classic pizza with tomato and mozzarella', 10.00, 'MAIN_COURSE', 'PUBLISHED'),
('d2d93f17-4567-8910-1112-314151617181', 'a6a52c73-9070-4128-a988-255383b941bc', 'Tiramisu', 'Italian dessert', 6.00, 'DESSERT', 'OUT_OF_STOCK'),
('d3da4018-5678-9101-1121-415161718191', 'b7b62d84-1234-5678-9101-112131415161', 'Sushi Roll', 'Fresh salmon roll', 12.00, 'MAIN_COURSE', 'DRAFT');
