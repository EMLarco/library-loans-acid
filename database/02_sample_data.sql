-- Datos de ejemplo (adaptados del repositorio de referencia)
INSERT INTO categories (name) VALUES
 ('Novela'), ('Ciencia ficción'), ('Historia'), ('Tecnología'), ('Poesía')
ON CONFLICT (name) DO NOTHING;

INSERT INTO authors (name, bio) VALUES
 ('Gabriel García Márquez', 'Escritor colombiano, Premio Nobel 1982'),
 ('Isaac Asimov', 'Autor de ciencia ficción y divulgación'),
 ('Jorge Icaza', 'Novelista ecuatoriano'),
 ('Robert C. Martin', 'Autor de Clean Code y Clean Architecture'),
 ('Pablo Neruda', 'Poeta chileno, Premio Nobel 1971'),
 ('Yuval Noah Harari', 'Historiador israelí');

INSERT INTO books (title, author_id, isbn, publication_date, genre_id, availability_status) VALUES
 ('Cien años de soledad',          1, '978-0307474728', '1967-05-30', 1, TRUE),
 ('El amor en los tiempos del cólera', 1, '978-0307387264', '1985-09-05', 1, TRUE),
 ('Fundación',                     2, '978-0553293357', '1951-06-01', 2, FALSE),
 ('Yo, robot',                     2, '978-0553382563', '1950-12-02', 2, TRUE),
 ('Huasipungo',                    3, '978-9978670157', '1934-01-01', 1, TRUE),
 ('Clean Code',                    4, '978-0132350884', '2008-08-01', 4, FALSE),
 ('Clean Architecture',            4, '978-0134494166', '2017-09-10', 4, TRUE),
 ('Veinte poemas de amor',         5, '978-0142437704', '1924-01-01', 5, TRUE),
 ('Sapiens',                       6, '978-0062316097', '2011-01-01', 3, TRUE),
 ('Homo Deus',                     6, '978-0062464316', '2015-01-01', 3, TRUE);

INSERT INTO members (name, contact_info, membership_id, join_date) VALUES
 ('Ana Torres',     'ana.torres@mail.com',    'MEM-001', '2025-01-10'),
 ('Carlos Mena',    'carlos.mena@mail.com',   'MEM-002', '2025-03-22'),
 ('Lucía Paredes',  'lucia.paredes@mail.com', 'MEM-003', '2025-06-15'),
 ('Diego Andrade',  'diego.andrade@mail.com', 'MEM-004', '2025-09-01'),
 ('Sofía Cevallos', 'sofia.cevallos@mail.com','MEM-005', '2026-02-12');

-- Préstamos: 2 activos (libros 3 y 6 no disponibles), 1 devuelto a tiempo,
-- 1 devuelto con retraso y multa pendiente (se paga con el servicio externo).
INSERT INTO loans (book_id, member_id, checkout_date, due_date, return_date, return_status, fine_amount, fine_paid) VALUES
 (3, 1, CURRENT_DATE - 5,  CURRENT_DATE + 9,  NULL, FALSE, 0, FALSE),
 (6, 2, CURRENT_DATE - 3,  CURRENT_DATE + 11, NULL, FALSE, 0, FALSE),
 (1, 3, CURRENT_DATE - 40, CURRENT_DATE - 26, CURRENT_DATE - 28, TRUE, 0, FALSE),
 (9, 4, CURRENT_DATE - 30, CURRENT_DATE - 16, CURRENT_DATE - 10, TRUE, 3.00, FALSE);
