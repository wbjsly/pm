package com.wh.config;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SqliteDateTypeHandler 测试")
class SqliteDateTypeHandlerTest {

    private SqliteDateTypeHandler handler;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @BeforeEach
    void setUp() {
        handler = new SqliteDateTypeHandler();
    }

    @Nested
    @DisplayName("setNonNullParameter")
    class SetNonNullParameter {

        @Test
        @DisplayName("以格式化字符串写入 PreparedStatement")
        void writesFormattedString() throws Exception {
            PreparedStatement ps = mock(PreparedStatement.class);
            Date d = FMT.parse("2026-01-05 10:30:00.123");
            handler.setNonNullParameter(ps, 2, d, JdbcType.DATE);
            verify(ps).setString(2, "2026-01-05 10:30:00.123");
        }
    }

    @Nested
    @DisplayName("getNullableResult")
    class GetNullableResult {

        @Test
        @DisplayName("按列名读取标准格式字符串")
        void byColumnName_parsesStandardFormat() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("created")).thenReturn("2026-01-05 10:30:00.123");
            Date d = handler.getNullableResult(rs, "created");
            assertEquals(FMT.parse("2026-01-05 10:30:00.123"), d);
        }

        @Test
        @DisplayName("按列索引读取 null 返回 null")
        void byColumnIndex_nullReturnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString(1)).thenReturn(null);
            assertNull(handler.getNullableResult(rs, 1));
        }

        @Test
        @DisplayName("旧版 epoch 毫秒回退解析")
        void epochMillisFallback() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("ts")).thenReturn("1785542400000");
            Date d = handler.getNullableResult(rs, "ts");
            assertEquals(new Date(1785542400000L), d);
        }

        @Test
        @DisplayName("无法解析时抛出 SQLException")
        void unparseable_throwsSqlException() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("bad")).thenReturn("not-a-date");
            assertThrows(SQLException.class, () -> handler.getNullableResult(rs, "bad"));
        }

        @Test
        @DisplayName("CallableStatement 按索引读取")
        void callableStatement() throws Exception {
            CallableStatement cs = mock(CallableStatement.class);
            when(cs.getString(3)).thenReturn("2026-06-01 00:00:00.000");
            Date d = handler.getNullableResult(cs, 3);
            assertEquals(FMT.parse("2026-06-01 00:00:00.000"), d);
        }
    }
}
