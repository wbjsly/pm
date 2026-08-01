package com.wh.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler 测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ServiceException → 返回对应 code")
    void serviceException_returnsCodeAndMessage() {
        R<Void> r = handler.handleServiceException(new ServiceException(403, "无权限"));
        assertEquals(403, r.getCode());
        assertEquals("无权限", r.getMessage());
    }

    @Test
    @DisplayName("方法参数校验异常 → 返回 400 与字段错误拼接")
    void validationException_returns400() {
        Object target = new Object();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "target");
        binding.addError(new FieldError("target", "name", "名称不能为空"));
        binding.addError(new FieldError("target", "age", "年龄必须为正"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        R<Void> r = handler.handleValidationException(ex);
        assertEquals(400, r.getCode());
        assertTrue(r.getMessage().contains("name: 名称不能为空"));
        assertTrue(r.getMessage().contains("age: 年龄必须为正"));
    }

    @Test
    @DisplayName("无字段错误时返回默认提示")
    void validationException_withoutFieldErrors_returnsDefault() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "target");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        R<Void> r = handler.handleValidationException(ex);
        assertEquals(400, r.getCode());
        assertEquals("参数校验失败", r.getMessage());
    }

    @Test
    @DisplayName("通用异常 → 返回 500 系统内部错误")
    void genericException_returns500() {
        R<Void> r = handler.handleException(new IllegalStateException("数据库连接失败"));
        assertEquals(500, r.getCode());
        assertTrue(r.getMessage().contains("数据库连接失败"));
    }
}
