package com.larco.biblioteca.repository.postgres;

import com.larco.biblioteca.model.Member;
import com.larco.biblioteca.repository.MemberRepository;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PostgresMemberRepository implements MemberRepository {
    private final DataSource ds;

    public PostgresMemberRepository(DataSource ds) { this.ds = ds; }

    @Override
    public List<Member> findAll() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT member_id, name, contact_info, membership_id, join_date FROM members ORDER BY member_id");
             ResultSet rs = ps.executeQuery()) {
            List<Member> out = new ArrayList<>();
            while (rs.next()) out.add(new Member(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    Jdbc.date(rs.getDate(5))));
            return out;
        } catch (SQLException e) { throw new TransactionFailedException(e.getMessage(), e); }
    }

    @Override
    public boolean exists(Connection c, int memberId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM members WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
