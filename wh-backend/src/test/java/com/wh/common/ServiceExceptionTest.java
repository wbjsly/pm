package com.wh.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServiceException 测试")
class ServiceExceptionTest {

    @Test
    @DisplayName("单参构造器默认 code=500")
    void singleArg_constructor_defaultsTo500() {
        ServiceException ex = new ServiceException("错误信息");
        assertEquals(500, ex.getCode());
        assertEquals("错误信息", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("双参构造器设置自定义 code")
    void twoArg_constructor_setsCode() {
        ServiceException ex = new ServiceException(404, "资源不存在");
        assertEquals(404, ex.getCode());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    @DisplayName("带 cause 构造器保留原因")
    void withCause_constructor_keepsCause() {
        RuntimeException cause = new RuntimeException("底层原因");
        ServiceException ex = new ServiceException("包装异常", cause);
        assertEquals(500, ex.getCode());
        assertSame(cause, ex.getCause());
    }
}
