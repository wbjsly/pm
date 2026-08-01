package com.wh.bo.system;

import com.wh.common.ServiceException;
import com.wh.dao.system.SysPositionDao;
import com.wh.dao.system.SysUserDao;
import com.wh.dao.system.SysUserRoleDao;
import com.wh.entity.system.SysPosition;
import com.wh.entity.system.SysUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserBo 测试")
class SysUserBoTest {

    @Mock private SysUserDao sysUserDao;
    @Mock private SysUserRoleDao sysUserRoleDao;
    @Mock private SysPositionDao sysPositionDao;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private SysUserBo sysUserBo;

    private SysUser user(String id, String username) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setRealName(username + "姓名");
        u.setStatus("1");
        u.setPositionId("pos-1");
        u.setDelFlag("0");
        return u;
    }

    @Nested
    @DisplayName("getRealNameMap")
    class GetRealNameMap {

        @Test
        @DisplayName("null 或空集合返回空 Map")
        void nullOrEmpty_returnsEmpty() {
            assertTrue(sysUserBo.getRealNameMap(null).isEmpty());
            assertTrue(sysUserBo.getRealNameMap(List.of()).isEmpty());
            assertTrue(sysUserBo.getRealNameMap(java.util.Arrays.asList(null, "")).isEmpty());
            verify(sysUserDao, never()).selectBatchIds(any());
        }

        @Test
        @DisplayName("批量查询返回姓名映射")
        void nonEmpty_returnsNameMap() {
            when(sysUserDao.selectBatchIds(any())).thenReturn(List.of(user("u1", "张三"), user("u2", "李四")));
            Map<String, String> map = sysUserBo.getRealNameMap(List.of("u1", "u2", "u3"));
            assertEquals("张三姓名", map.get("u1"));
            assertEquals("李四姓名", map.get("u2"));
            assertNull(map.get("u3"));
        }
    }

    @Nested
    @DisplayName("countUsersByPositionId")
    class CountUsers {

        @Test
        @DisplayName("统计岗位用户数")
        void countsUsers() {
            when(sysUserDao.selectCount(any())).thenReturn(3L);
            assertEquals(3L, sysUserBo.countUsersByPositionId("pos-1"));
        }
    }

    @Nested
    @DisplayName("getUserRoleCodes")
    class GetUserRoleCodes {

        @Test
        @DisplayName("返回角色编码列表")
        void returnsRoleCodes() {
            when(sysUserRoleDao.getUserRoles("u1")).thenReturn(
                    List.of(Map.of("ROLE_CODE", "ROLE_PM"), Map.of("ROLE_CODE", "ROLE_ADMIN")));
            assertEquals(List.of("ROLE_PM", "ROLE_ADMIN"), sysUserBo.getUserRoleCodes("u1"));
        }
    }

    @Nested
    @DisplayName("pageUsers")
    class PageUsers {

        @Test
        @DisplayName("无条件分页返回记录")
        void noFilter_returnsPage() {
            when(sysUserDao.selectPage(any(), any())).thenAnswer(inv -> {
                var page = inv.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
                page.setTotal(1);
                page.setRecords(List.of(user("u1", "admin")));
                return page;
            });
            SysPosition pos = new SysPosition();
            pos.setId("pos-1");
            pos.setName("测试岗位");
            when(sysPositionDao.selectBatchIds(any())).thenReturn(List.of(pos));
            when(sysUserRoleDao.getRoleCodesByUserIds(any())).thenReturn(
                    List.of(Map.of("USER_ID", "u1", "ROLE_CODE", "ROLE_ADMIN")));

            Map<String, Object> result = sysUserBo.pageUsers(1, 10, null, null, null);

            assertEquals(1L, result.get("total"));
            List<?> records = (List<?>) result.get("records");
            assertEquals(1, records.size());
            Map<?, ?> first = (Map<?, ?>) records.get(0);
            assertEquals("admin", first.get("username"));
            assertEquals(List.of("ROLE_ADMIN"), first.get("roles"));
        }

        @Test
        @DisplayName("按关键词/状态/岗位筛选")
        void withFilters_queries() {
            when(sysUserDao.selectPage(any(), any())).thenAnswer(inv -> {
                var page = inv.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
                page.setTotal(0);
                page.setRecords(List.of());
                return page;
            });
            Map<String, Object> result = sysUserBo.pageUsers(1, 10, "张", "1", "pos-1");
            assertEquals(0L, result.get("total"));
        }
    }

    @Nested
    @DisplayName("getUserDetail")
    class GetUserDetail {

        @Test
        @DisplayName("存在用户返回详情含角色")
        void existingUser_returnsDetail() {
            when(sysUserDao.selectById("u1")).thenReturn(user("u1", "admin"));
            SysPosition pos = new SysPosition();
            pos.setId("pos-1");
            pos.setName("测试岗位");
            when(sysPositionDao.selectBatchIds(any())).thenReturn(List.of(pos));
            when(sysUserRoleDao.getUserRoles("u1")).thenReturn(List.of(Map.of("ROLE_CODE", "ROLE_ADMIN")));

            Map<String, Object> detail = sysUserBo.getUserDetail("u1");
            assertEquals("u1", detail.get("id"));
            assertEquals(List.of("ROLE_ADMIN"), detail.get("roles"));
        }

        @Test
        @DisplayName("不存在用户抛 404")
        void missingUser_throws404() {
            when(sysUserDao.selectById("x")).thenReturn(null);
            ServiceException ex = assertThrows(ServiceException.class, () -> sysUserBo.getUserDetail("x"));
            assertEquals(404, ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("更新字段与角色")
        void updatesFieldsAndRoles() {
            SysUser u = user("u1", "admin");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            when(sysUserRoleDao.deleteByUserId("u1")).thenReturn(1);
            when(sysUserRoleDao.insertUserRole(any(), any(), any())).thenReturn(1);

            sysUserBo.updateUser("u1", Map.of(
                    "nickName", "新昵称",
                    "email", "a@b.com",
                    "status", "0",
                    "roleIds", List.of("r1", "r2")));

            assertEquals("新昵称", u.getNickName());
            assertEquals("0", u.getStatus());
            verify(sysUserRoleDao).deleteByUserId("u1");
            verify(sysUserRoleDao, times(2)).insertUserRole(any(), eq("u1"), any());
        }

        @Test
        @DisplayName("不存在用户抛 404")
        void missingUser_throws404() {
            when(sysUserDao.selectById("x")).thenReturn(null);
            assertThrows(ServiceException.class, () -> sysUserBo.updateUser("x", Map.of()));
        }
    }

    @Nested
    @DisplayName("deleteUser / resetPassword")
    class DeleteAndReset {

        @Test
        @DisplayName("逻辑删除用户")
        void deleteUser_logicalDelete() {
            SysUser u = user("u1", "admin");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            sysUserBo.deleteUser("u1");
            assertEquals("1", u.getDelFlag());
            verify(sysUserDao).updateById(u);
        }

        @Test
        @DisplayName("空密码抛 400")
        void resetPassword_emptyThrows400() {
            SysUser u = user("u1", "admin");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> sysUserBo.resetPassword("u1", ""));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("重置密码加密保存")
        void resetPassword_encodesAndSaves() {
            SysUser u = user("u1", "admin");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            when(passwordEncoder.encode("NewPass")).thenReturn("hashed");
            sysUserBo.resetPassword("u1", "NewPass");
            assertEquals("hashed", u.getPassword());
            verify(sysUserDao).updateById(u);
        }
    }

    @Nested
    @DisplayName("补充边界")
    class ExtraBranchTests {

        @Test
        @DisplayName("pageUsers - 空字符串筛选参数")
        void pageUsers_emptyStrings() {
            when(sysUserDao.selectPage(any(), any())).thenAnswer(inv -> {
                var page = inv.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
                page.setTotal(0);
                page.setRecords(List.of());
                return page;
            });
            Map<String, Object> result = sysUserBo.pageUsers(1, 10, "", "", "");
            assertEquals(0L, result.get("total"));
        }

        @Test
        @DisplayName("updateUser - 更新全部字段")
        void updateUser_allFields() {
            SysUser u = user("u1", "admin");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            sysUserBo.updateUser("u1", Map.of(
                    "realName", "真实名",
                    "phone", "13800000000",
                    "positionId", "pos-9"));
            assertEquals("真实名", u.getRealName());
            assertEquals("13800000000", u.getPhone());
            assertEquals("pos-9", u.getPositionId());
        }

        @Test
        @DisplayName("resetPassword - null 密码抛 400")
        void resetPassword_nullThrows400() {
            SysUser u = user("u1", "admin");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> sysUserBo.resetPassword("u1", null));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("getUserDetail - 逻辑删除用户抛 404")
        void getUserDetail_logicallyDeleted_throws404() {
            SysUser u = user("u1", "admin");
            u.setDelFlag("1");
            when(sysUserDao.selectById("u1")).thenReturn(u);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> sysUserBo.getUserDetail("u1"));
            assertEquals(404, ex.getCode());
        }
    }
}
