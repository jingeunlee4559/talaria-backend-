package com.example.talaria.backend.repository.user;

import com.example.talaria.backend.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String TABLE = "tl_users";
    private static final String PK    = "user_id";

    public Map<String, Object> findById(long id) {
        var sql = "SELECT * FROM " + TABLE + " WHERE " + PK + " = :id";
        var params = new MapSqlParameterSource("id", id);
        try {
            return jdbc.queryForMap(sql, params);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException(id);
        }
    }

    public long countAll() {
        var sql = "SELECT COUNT(*) FROM " + TABLE;
        Long c = jdbc.getJdbcTemplate().queryForObject(sql, Long.class);
        return c == null ? 0L : c;
    }

    public List<Map<String, Object>> findPage(int limit, int offset) {
        var sql = "SELECT * FROM " + TABLE + " ORDER BY " + PK + " DESC LIMIT :limit OFFSET :offset";
        var params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbc.query(sql, params, (rs, rowNum) -> {
            var md = rs.getMetaData();
            var map = new java.util.LinkedHashMap<String, Object>(md.getColumnCount());
            for (int i = 1; i <= md.getColumnCount(); i++) {
                map.put(md.getColumnLabel(i), rs.getObject(i));
            }
            return map;
        });
    }
}
