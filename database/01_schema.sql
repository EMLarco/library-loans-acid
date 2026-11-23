-- =====================================================================
-- Library Management - Esquema PostgreSQL
-- Basado en: https://github.com/omr-ql/library_management_database
-- Adaptaciones para el taller (documentadas en README.md):
--   * books.active           -> eliminación lógica
--   * loans.fine_amount/paid -> multa que se paga por el servicio externo
--   * CHECKs adicionales     -> reglas de consistencia
--   * ux_loans_active_book   -> un libro no puede tener 2 préstamos activos
--   * audit_log              -> tabla de auditoría (Proxy / Observer)
-- =====================================================================

CREATE TABLE IF NOT EXISTS categories (
    genre_id   SERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS authors (
    author_id  SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    bio        TEXT
);

CREATE TABLE IF NOT EXISTS books (
    book_id             SERIAL PRIMARY KEY,
    title               VARCHAR(255) NOT NULL CHECK (length(trim(title)) > 0),
    author_id           INT NOT NULL REFERENCES authors(author_id),
    isbn                VARCHAR(20) UNIQUE,
    publication_date    DATE,
    genre_id            INT REFERENCES categories(genre_id),
    availability_status BOOLEAN NOT NULL DEFAULT TRUE,
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS members (
    member_id      SERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    contact_info   VARCHAR(255),
    membership_id  VARCHAR(50) UNIQUE,
    join_date      DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS loans (
    loan_id        SERIAL PRIMARY KEY,
    book_id        INT NOT NULL REFERENCES books(book_id),
    member_id      INT NOT NULL REFERENCES members(member_id),
    checkout_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date       DATE NOT NULL,
    return_date    DATE,
    return_status  BOOLEAN NOT NULL DEFAULT FALSE,
    fine_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    fine_paid      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_loans_due_after_checkout   CHECK (due_date > checkout_date),
    CONSTRAINT ck_loans_return_after_checkout CHECK (return_date IS NULL OR return_date >= checkout_date),
    CONSTRAINT ck_loans_fine_non_negative    CHECK (fine_amount >= 0)
);

-- Regla de negocio a nivel de BD: un libro solo puede tener UN préstamo activo.
CREATE UNIQUE INDEX IF NOT EXISTS ux_loans_active_book ON loans(book_id) WHERE return_status = FALSE;

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id      BIGSERIAL PRIMARY KEY,
    event_type    VARCHAR(60)  NOT NULL,
    performed_by  VARCHAR(100) NOT NULL,
    details       TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
