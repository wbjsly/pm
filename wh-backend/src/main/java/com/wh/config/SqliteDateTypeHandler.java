package com.wh.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SQLite JDBC stores setTimestamp as epoch millis but reads getTimestamp
 * expecting "yyyy-MM-dd HH:mm:ss.SSS". This handler forces string-based
 * round-tripping for consistent date persistence on SQLite.
 */
public class SqliteDateTypeHandler extends BaseTypeHandler<Date> {

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, FMT.format(parameter));
    }

    @Override
    public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private Date parse(String val) throws SQLException {
        if (val == null) {
            return null;
        }
        try {
            return FMT.parse(val);
        } catch (Exception e) {
            // Fallback: legacy epoch millis
            try {
                return new Date(Long.parseLong(val));
            } catch (NumberFormatException nfe) {
                throw new SQLException("Cannot parse date: " + val, e);
            }
        }
    }
}
