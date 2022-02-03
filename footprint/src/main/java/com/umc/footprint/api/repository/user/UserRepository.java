package com.umc.footprint.api.repository.user;

import com.umc.footprint.api.entity.user.OAuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Optional<OAuthUser> findByUserId(String userId) {
        String findUserIdQuery = "select userIdx, userId, username, email, emailVerifiedYn, providerType, roleType, createAt, updateAt from USER where userId = ?";

        List<OAuthUser> result = jdbcTemplate.query(findUserIdQuery, userRowMapper(), userId);
        return result.stream().findAny();
    }

    public OAuthUser saveAndFlush(OAuthUser user) {
        String saveQuery = "insert into USER(userId, username, password, email, emailVerifiedYn, providerType, roleType, createAt, updateAt) values (?,?,?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement preparedStatement = con.prepareStatement(saveQuery, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, user.getUserId());
                preparedStatement.setString(2, user.getUsername());
                preparedStatement.setString(3, user.getPassword());
                preparedStatement.setString(4, user.getEmail());
                preparedStatement.setString(5, user.getEmailVerifiedYn());
                preparedStatement.setString(6, user.getProviderType().toString());
                preparedStatement.setString(7, user.getRoleType().toString());
                preparedStatement.setTimestamp(8, Timestamp.valueOf(user.getCreatedAt()));
                preparedStatement.setTimestamp(9, Timestamp.valueOf(user.getModifiedAt()));
                return preparedStatement;
            }
        }, keyHolder);

        user.setUserSeq(keyHolder.getKey().longValue());
        return user;
    }

    private RowMapper<OAuthUser> userRowMapper() {
        return (rs, rowNum) -> new OAuthUser(
                rs.getLong("userIdx"),
                rs.getString("userId"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("emailVerifiedYn"),
                rs.getString("providerType"),
                rs.getString("roleType"),
                rs.getTimestamp("createAt").toLocalDateTime(),
                rs.getTimestamp("updateAt").toLocalDateTime()
        );
    }

}
