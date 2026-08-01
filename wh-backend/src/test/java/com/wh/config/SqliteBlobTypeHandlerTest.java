package com.wh.config;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

@DisplayName("SqliteBlobTypeHandler 测试")
class SqliteBlobTypeHandlerTest {

    private SqliteBlobTypeHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SqliteBlobTypeHandler();
    }

    @Test
    @DisplayName("setNonNullParameter 以字节数组写入")
    void setNonNullParameter_writesBytes() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        byte[] data = {1, 2, 3};
        handler.setNonNullParameter(ps, 1, data, JdbcType.BLOB);
        verify(ps).setBytes(1, data);
    }

    @Test
    @DisplayName("按列名读取字节数组")
    void byColumnName_readsBytes() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        byte[] data = {9, 8, 7};
        when(rs.getBytes("blob")).thenReturn(data);
        assertArrayEquals(data, handler.getNullableResult(rs, "blob"));
    }

    @Test
    @DisplayName("按列索引读取字节数组")
    void byColumnIndex_readsBytes() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        byte[] data = {4, 5, 6};
        when(rs.getBytes(2)).thenReturn(data);
        assertArrayEquals(data, handler.getNullableResult(rs, 2));
    }

    @Test
    @DisplayName("CallableStatement 读取字节数组")
    void callableStatement_readsBytes() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        byte[] data = {0, 1, 0};
        when(cs.getBytes(3)).thenReturn(data);
        assertArrayEquals(data, handler.getNullableResult(cs, 3));
    }
}
