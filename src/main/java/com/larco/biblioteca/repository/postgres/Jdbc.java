package com.larco.biblioteca.repository.postgres;

import java.sql.Date;
import java.time.LocalDate;

final class Jdbc {
    private Jdbc() { }
    static LocalDate date(Date d) { return d == null ? null : d.toLocalDate(); }
    static Date sql(LocalDate d) { return d == null ? null : Date.valueOf(d); }
}
