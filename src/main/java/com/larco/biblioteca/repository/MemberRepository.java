package com.larco.biblioteca.repository;

import com.larco.biblioteca.model.Member;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface MemberRepository {
    List<Member> findAll();
    boolean exists(Connection c, int memberId) throws SQLException;
}
