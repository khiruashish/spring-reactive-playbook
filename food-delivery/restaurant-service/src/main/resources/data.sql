INSERT INTO restaurants (id, name, cuisine, address, open) VALUES
    (1, 'Spice Route',   'Indian',  '12 MG Road, Bengaluru',        TRUE),
    (2, 'Napoli Express','Italian', '4 Linking Road, Mumbai',       TRUE),
    (3, 'Wok This Way',  'Chinese', '88 Park Street, Kolkata',      TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO menu_items (id, restaurant_id, name, description, price_paise, available) VALUES
    (1, 1, 'Paneer Butter Masala', 'Cottage cheese in tomato-butter gravy', 24900, TRUE),
    (2, 1, 'Garlic Naan',          'Tandoor-baked flatbread',                 5900, TRUE),
    (3, 1, 'Chicken Biryani',      'Dum-cooked basmati with spices',         29900, TRUE),
    (4, 2, 'Margherita Pizza',     'San Marzano, fior di latte, basil',      34900, TRUE),
    (5, 2, 'Tiramisu',             'Mascarpone, espresso, cocoa',            19900, TRUE),
    (6, 3, 'Hakka Noodles',        'Wok-tossed noodles with vegetables',     18900, TRUE),
    (7, 3, 'Chilli Chicken',       'Indo-Chinese classic',                   25900, TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval('restaurants_id_seq', (SELECT MAX(id) FROM restaurants));
SELECT setval('menu_items_id_seq',  (SELECT MAX(id) FROM menu_items));
