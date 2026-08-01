package com.wh.bo.system;

import com.wh.common.ServiceException;
import com.wh.dao.system.SysCostQuotaDao;
import com.wh.dao.system.SysCostYearDao;
import com.wh.dao.system.SysPositionDao;
import com.wh.entity.system.SysCostQuota;
import com.wh.entity.system.SysCostYear;
import com.wh.entity.system.SysPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysCostQuotaBo 测试")
class SysCostQuotaBoTest {

    @Mock private SysPositionDao positionDao;
    @Mock private SysCostYearDao yearDao;
    @Mock private SysCostQuotaDao quotaDao;
    @Mock private SysUserBo sysUserBo;
    @InjectMocks private SysCostQuotaBo bo;

    private SysPosition pos(String id, String name) {
        SysPosition p = new SysPosition();
        p.setId(id);
        p.setName(name);
        p.setIsDefault("0");
        return p;
    }

    private SysCostYear year(String id, String start, String end) {
        SysCostYear y = new SysCostYear();
        y.setId(id);
        y.setName(id);
        y.setStartDate(start);
        y.setEndDate(end);
        return y;
    }

    private SysCostQuota quota(String id, String positionId, int versionNo, String rate) {
        SysCostQuota q = new SysCostQuota();
        q.setId(id);
        q.setPositionId(positionId);
        q.setYearId("year-1");
        q.setVersionNo(versionNo);
        q.setDailyRate(new BigDecimal(rate));
        q.setEffectiveDate(LocalDate.now().toString());
        return q;
    }

    @Nested
    @DisplayName("岗位管理")
    class PositionTests {

        @Test
        @DisplayName("listPositions 按排序返回")
        void listPositions() {
            when(positionDao.selectList(any())).thenReturn(List.of(pos("p1", "开发")));
            assertEquals(1, bo.listPositions().size());
        }

        @Test
        @DisplayName("createPosition 清空 id 并设非默认")
        void createPosition() {
            SysPosition p = pos("p1", "测试");
            bo.createPosition(p);
            assertNull(p.getId());
            assertEquals("0", p.getIsDefault());
            verify(positionDao).insert(p);
        }

        @Test
        @DisplayName("updatePosition 设置 id 并更新")
        void updatePosition() {
            SysPosition p = pos(null, "新岗位");
            bo.updatePosition("p9", p);
            assertEquals("p9", p.getId());
            verify(positionDao).updateById(p);
        }

        @Test
        @DisplayName("deletePosition - 不存在抛 404")
        void deletePosition_notFound() {
            when(positionDao.selectById("x")).thenReturn(null);
            ServiceException ex = assertThrows(ServiceException.class, () -> bo.deletePosition("x"));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("deletePosition - 已有定额不可删")
        void deletePosition_hasQuota() {
            when(positionDao.selectById("p1")).thenReturn(pos("p1", "开发"));
            when(quotaDao.selectCount(any())).thenReturn(1L);
            ServiceException ex = assertThrows(ServiceException.class, () -> bo.deletePosition("p1"));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("deletePosition - 岗位下有用户不可删")
        void deletePosition_hasUsers() {
            when(positionDao.selectById("p1")).thenReturn(pos("p1", "开发"));
            when(quotaDao.selectCount(any())).thenReturn(0L);
            when(sysUserBo.countUsersByPositionId("p1")).thenReturn(2L);
            ServiceException ex = assertThrows(ServiceException.class, () -> bo.deletePosition("p1"));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("deletePosition - 成功删除")
        void deletePosition_success() {
            when(positionDao.selectById("p1")).thenReturn(pos("p1", "开发"));
            when(quotaDao.selectCount(any())).thenReturn(0L);
            when(sysUserBo.countUsersByPositionId("p1")).thenReturn(0L);
            bo.deletePosition("p1");
            verify(positionDao).deleteById("p1");
        }
    }

    @Nested
    @DisplayName("年份管理")
    class YearTests {

        @Test
        @DisplayName("createYear - 无上一年时不复制")
        void createYear_noPrev() {
            SysCostYear y = year("y2026", "2026-01-01", "2026-12-31");
            when(yearDao.selectOne(any())).thenReturn(null);
            SysCostYear result = bo.createYear(y);
            assertNull(result.getId());
            verify(yearDao).insert(y);
            verify(quotaDao, never()).insert(any());
        }

        @Test
        @DisplayName("createYear - 有上一年时复制最新定额")
        void createYear_withPrev() {
            SysCostYear y = year("y2026", "2026-01-01", "2026-12-31");
            when(yearDao.selectOne(any())).thenReturn(year("y2025", "2025-01-01", "2025-12-31"));
            when(positionDao.selectList(any())).thenReturn(List.of(pos("p1", "开发")));
            when(quotaDao.selectList(any())).thenReturn(List.of(quota("q1", "p1", 3, "1200")));
            bo.createYear(y);
            verify(yearDao).insert(y);
            verify(quotaDao).insert(any(SysCostQuota.class));
        }

        @Test
        @DisplayName("deleteYear - 不存在抛 404")
        void deleteYear_notFound() {
            when(yearDao.selectById("x")).thenReturn(null);
            assertThrows(ServiceException.class, () -> bo.deleteYear("x"));
        }

        @Test
        @DisplayName("deleteYear - 已有定额不可删")
        void deleteYear_hasQuota() {
            when(yearDao.selectById("y1")).thenReturn(year("y1", "2026-01-01", "2026-12-31"));
            when(quotaDao.selectCount(any())).thenReturn(1L);
            assertThrows(ServiceException.class, () -> bo.deleteYear("y1"));
        }

        @Test
        @DisplayName("deleteYear - 成功删除")
        void deleteYear_success() {
            when(yearDao.selectById("y1")).thenReturn(year("y1", "2026-01-01", "2026-12-31"));
            when(quotaDao.selectCount(any())).thenReturn(0L);
            bo.deleteYear("y1");
            verify(yearDao).deleteById("y1");
        }

        @Test
        @DisplayName("getDefaultStartDate - 有最新年份用其结束日")
        void getDefaultStartDate_withLatest() {
            when(yearDao.selectOne(any())).thenReturn(year("y1", "2026-01-01", "2026-12-31"));
            assertEquals("2026-12-31", bo.getDefaultStartDate().get("defaultStartDate"));
        }

        @Test
        @DisplayName("getDefaultStartDate - 无年份用当前日期")
        void getDefaultStartDate_withoutLatest() {
            when(yearDao.selectOne(any())).thenReturn(null);
            assertEquals(LocalDate.now().toString(), bo.getDefaultStartDate().get("defaultStartDate"));
        }
    }

    @Nested
    @DisplayName("定额管理")
    class QuotaTests {

        @Test
        @DisplayName("listQuotas - 无定额时填充 null")
        void listQuotas_withoutQuota() {
            when(positionDao.selectList(any())).thenReturn(List.of(pos("p1", "开发")));
            when(quotaDao.selectList(any())).thenReturn(List.of());
            List<Map<String, Object>> result = bo.listQuotas("year-1");
            assertEquals(1, result.size());
            assertNull(result.get(0).get("quotaId"));
        }

        @Test
        @DisplayName("listQuotas - 有定额取最新版本")
        void listQuotas_withQuota() {
            when(positionDao.selectList(any())).thenReturn(List.of(pos("p1", "开发")));
            when(quotaDao.selectList(any())).thenReturn(
                    List.of(quota("q1", "p1", 1, "1000"), quota("q2", "p1", 5, "1500")));
            List<Map<String, Object>> result = bo.listQuotas("year-1");
            assertEquals("q2", result.get(0).get("quotaId"));
            assertEquals("1500", result.get(0).get("dailyRate").toString());
        }

        @Test
        @DisplayName("adjustQuota - 同日冲突抛 400")
        void adjustQuota_conflict() {
            when(quotaDao.selectCount(any())).thenReturn(1L);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> bo.adjustQuota(Map.of("positionId", "p1", "yearId", "y1", "dailyRate", "1000")));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("adjustQuota - 无历史版本从 1 开始")
        void adjustQuota_firstVersion() {
            when(quotaDao.selectCount(any())).thenReturn(0L);
            when(quotaDao.selectOne(any())).thenReturn(null);
            SysCostQuota q = bo.adjustQuota(Map.of(
                    "positionId", "p1", "yearId", "y1", "dailyRate", "1200",
                    "effectiveDate", "2026-01-01", "changeReason", "调薪"));
            assertEquals(1, q.getVersionNo());
            assertEquals("1200", q.getDailyRate().toString());
            verify(quotaDao).insert(q);
        }

        @Test
        @DisplayName("adjustQuota - 有历史版本递增")
        void adjustQuota_nextVersion() {
            when(quotaDao.selectCount(any())).thenReturn(0L);
            when(quotaDao.selectOne(any())).thenReturn(quota("q1", "p1", 4, "1000"));
            SysCostQuota q = bo.adjustQuota(Map.of(
                    "positionId", "p1", "yearId", "y1", "dailyRate", "1300"));
            assertEquals(5, q.getVersionNo());
            assertEquals(LocalDate.now().toString(), q.getEffectiveDate());
        }

        @Test
        @DisplayName("getCurrentRate - 无匹配年份返回空")
        void getCurrentRate_noYear() {
            when(yearDao.selectOne(any())).thenReturn(null);
            assertTrue(bo.getCurrentRate("p1").isEmpty());
        }

        @Test
        @DisplayName("getCurrentRate - 无匹配定额返回空")
        void getCurrentRate_noQuota() {
            when(yearDao.selectOne(any())).thenReturn(year("y1", "2020-01-01", "2030-12-31"));
            when(quotaDao.selectOne(any())).thenReturn(null);
            assertTrue(bo.getCurrentRate("p1").isEmpty());
        }

        @Test
        @DisplayName("getCurrentRate - 计算时薪")
        void getCurrentRate_computesRate() {
            when(yearDao.selectOne(any())).thenReturn(year("y1", "2020-01-01", "2030-12-31"));
            when(quotaDao.selectOne(any())).thenReturn(quota("q1", "p1", 2, "800"));
            when(positionDao.selectById("p1")).thenReturn(pos("p1", "开发"));
            Map<String, Object> result = bo.getCurrentRate("p1");
            assertEquals("开发", result.get("positionName"));
            assertEquals(new BigDecimal("100.00"), result.get("costRate"));
            assertEquals("800", result.get("dailyRate").toString());
        }

        @Test
        @DisplayName("getCurrentRate - 岗位不存在时 positionName 为 null")
        void getCurrentRate_noPosition() {
            when(yearDao.selectOne(any())).thenReturn(year("y1", "2020-01-01", "2030-12-31"));
            when(quotaDao.selectOne(any())).thenReturn(quota("q1", "p1", 2, "800"));
            when(positionDao.selectById("p1")).thenReturn(null);
            Map<String, Object> result = bo.getCurrentRate("p1");
            assertNull(result.get("positionName"));
        }

        @Test
        @DisplayName("getHistory 返回版本历史含创建人")
        void getHistory() {
            SysCostQuota q = quota("q1", "p1", 1, "1000");
            q.setCreateBy("creator");
            when(quotaDao.selectList(any())).thenReturn(List.of(q));
            when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>(Map.of("creator", "创建人")));
            List<Map<String, Object>> history = bo.getHistory("p1", "y1");
            assertEquals(1, history.size());
            assertEquals("q1", history.get(0).get("id"));
        }

        @Test
        @DisplayName("compareVersions 计算差价")
        void compareVersions() {
            when(quotaDao.selectById("q1")).thenReturn(quota("q1", "p1", 1, "1000"));
            when(quotaDao.selectById("q2")).thenReturn(quota("q2", "p1", 2, "1200"));
            Map<String, Object> result = bo.compareVersions("q1", "q2");
            assertEquals(new BigDecimal("-200"), result.get("rateDiff"));
        }
    }
}
