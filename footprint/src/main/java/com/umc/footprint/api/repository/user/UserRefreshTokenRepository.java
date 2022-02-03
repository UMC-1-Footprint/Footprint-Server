package com.umc.footprint.api.repository.user;

import com.umc.footprint.api.entity.user.OAuthUser;
import com.umc.footprint.api.entity.user.UserRefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
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
import javax.swing.text.html.Option;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRefreshTokenRepository {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public Optional<UserRefreshToken> findByUserId(String userId) {
        System.out.println("userId = " + userId);
        String findUserByIdQuery = "select refreshTokenIdx, refreshToken, userId from UserRefreshToken where userId = ?";

        List<UserRefreshToken> result = jdbcTemplate.query(findUserByIdQuery, userRefreshRowMapper(), userId);
        return result.stream().findAny();
    }

    public Optional<UserRefreshToken> findByUserIdAndRefreshToken(String userId, String refreshToken) {
        String findUserByIdAndRefreshTokenQuery = "select * from UserRefreshToken where userId = ? and refreshToken = ?";

        List<UserRefreshToken> result = jdbcTemplate.query(findUserByIdAndRefreshTokenQuery, userRefreshRowMapper(), userId, refreshToken);
        return result.stream().findAny();
    }


    public void saveAndFlush(UserRefreshToken userRefreshToken) {
        String saveQuery = "insert into UserRefreshToken(refreshToken, userId) values (?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement preparedStatement = con.prepareStatement(saveQuery, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, userRefreshToken.getRefreshToken());
                preparedStatement.setString(2, userRefreshToken.getUserId());
                return preparedStatement;
            }
        }, keyHolder);

        userRefreshToken.setRefreshTokenSeq(keyHolder.getKey().longValue());
    }

    private RowMapper<UserRefreshToken> userRefreshRowMapper() {
        return (rs, rowNum) -> new UserRefreshToken(
                rs.getLong("refreshTokenIdx"),
                rs.getString("refreshToken"),
                rs.getString("userId")
        );
    }
}
