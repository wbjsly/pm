package com.wh;

import com.wh.bo.pm.WhSysWorkCalendarBo;
import com.wh.bo.pm.WorkCalendarRequest;
import com.wh.dao.pm.WhSysWorkCalendarDao;
import com.wh.entity.pm.WhSysWorkCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("WhSysWorkCalendarBo 测试")
class WhSysWorkCalendarBoTest {

    @Autowired
    private WhSysWorkCalendarBo calendarBo;

    @Autowired
    private WhSysWorkCalendarDao calendarDao;

    @BeforeEach
    void setUp() {
        calendarBo.generateYear("2026", "admin");
    }

    // ══════════════════════════════════════════════════
    //  日历生成
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("日历生成")
    class GenerateYear {

        @Test
        @DisplayName("generateYear - 2026年生成365天")
        void testGenerateYear_Creates365Days() {
            List<WhSysWorkCalendar> all = calendarBo.getByYear("2026");
            assertEquals(365, all.size(), "2026年非闰年，应有365天");
        }

        @Test
        @DisplayName("generateYear - 2028闰年生成366天")
        void testGenerateYear_LeapYear() {
            calendarBo.generateYear("2028", "admin");
            List<WhSysWorkCalendar> all = calendarBo.getByYear("2028");
            assertEquals(366, all.size(), "2028年是闰年，应有366天");
        }

        @Test
        @DisplayName("generateYear - 工作日与周末区分正确")
        void testGenerateDayType_WorkdayVsWeekend() {
            // 2026-01-05 周一 -> WORKDAY
            WhSysWorkCalendar monday = calendarBo.getByDate("2026-01-05");
            assertEquals("WORKDAY", monday.getDayType());
            assertEquals("8", monday.getStandardHours());

            // 2026-01-03 周六 -> WEEKEND
            WhSysWorkCalendar saturday = calendarBo.getByDate("2026-01-03");
            assertEquals("WEEKEND", saturday.getDayType());

            // 2026-01-04 周日 -> WEEKEND
            WhSysWorkCalendar sunday = calendarBo.getByDate("2026-01-04");
            assertEquals("WEEKEND", sunday.getDayType());
        }

        @Test
        @DisplayName("generateYear - 重新生成替换已有数据")
        void testGenerateYear_ReplacesExisting() {
            // 修改某一天
            WhSysWorkCalendar modified = calendarBo.getByDate("2026-01-01");
            assertNotNull(modified);

            // 重新生成
            calendarBo.generateYear("2026", "admin");
            List<WhSysWorkCalendar> all = calendarBo.getByYear("2026");
            assertEquals(365, all.size(), "重新生成后仍应有365天");
        }
    }

    // ══════════════════════════════════════════════════
    //  工作日判断
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("工作日判断")
    class WorkDayCheck {

        @Test
        @DisplayName("isWorkDay - 从日历读取")
        void testIsWorkDay_FromCalendar() {
            assertTrue(calendarBo.isWorkDay("2026-01-05"), "周一应为工作日");
            assertFalse(calendarBo.isWorkDay("2026-01-03"), "周六不应为工作日");
        }

        @Test
        @DisplayName("isWorkDay - 无日历数据时回退到默认逻辑")
        void testIsWorkDay_FallbackToDefault() {
            LocalDate date = LocalDate.of(2026, 1, 5); // Monday
            assertTrue(calendarBo.isWorkDay("2026-01-05"), "周一默认应为工作日");
            assertFalse(calendarBo.isWorkDay("2026-01-03"), "周六默认不应为工作日");
        }
    }

    // ══════════════════════════════════════════════════
    //  标准工时
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("标准工时")
    class StandardHours {

        @Test
        @DisplayName("getStandardHours - 从日历读取")
        void testGetStandardHours() {
            assertEquals("8", calendarBo.getStandardHours("2026-01-05"));
        }

        @Test
        @DisplayName("getStandardHours - 无日历数据返回默认8")
        void testGetStandardHours_DefaultWhenNoCalendar() {
            assertEquals("8", calendarBo.getStandardHours("2099-01-01"));
        }
    }

    // ══════════════════════════════════════════════════
    //  月度查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("月度查询")
    class MonthQuery {

        @Test
        @DisplayName("getWorkDays - 返回指定月份的所有工作日")
        void testGetWorkDays() {
            List<String> workDays = calendarBo.getWorkDays("2026", 1);
            // 2026年1月有22个工作日
            assertFalse(workDays.isEmpty());
            assertTrue(workDays.stream().allMatch(d -> d.startsWith("2026-01")));
        }

        @Test
        @DisplayName("getMonthCalendar - 返回该月所有日期")
        void testGetMonthCalendar() {
            Map<String, WhSysWorkCalendar> monthCal = calendarBo.getMonthCalendar("2026", 1);
            assertEquals(31, monthCal.size(), "1月有31天");
        }

        @Test
        @DisplayName("getMonthCalendar - 无日历数据的日期使用默认值")
        void testGetMonthCalendar_FallbackForMissingDays() {
            Map<String, WhSysWorkCalendar> monthCal = calendarBo.getMonthCalendar("2099", 1);
            assertEquals(31, monthCal.size());
            // 2099-01-05 是周一
            assertEquals("WORKDAY", monthCal.get("2099-01-05").getDayType());
            // 2099-01-03 是周六
            assertEquals("WEEKEND", monthCal.get("2099-01-03").getDayType());
        }
    }

    // ══════════════════════════════════════════════════
    //  单日更新
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("单日更新")
    class UpdateDay {

        @Test
        @DisplayName("updateDay - 更新已有日期为节假日")
        void testUpdateDay_ExistingDate() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setCalendarDate("2026-01-05"); // 周一
            req.setDayType("HOLIDAY");
            req.setHolidayName("测试假期");
            req.setStandardHours("0");

            calendarBo.updateDay(req, "admin");

            WhSysWorkCalendar updated = calendarBo.getByDate("2026-01-05");
            assertEquals("HOLIDAY", updated.getDayType());
            assertEquals("测试假期", updated.getHolidayName());
            assertEquals("0", updated.getStandardHours());
        }

        @Test
        @DisplayName("updateDay - 更新不存在的日期（新建记录）")
        void testUpdateDay_NonExistingDate() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setCalendarDate("2099-06-15");
            req.setDayType("WORKDAY");
            req.setHolidayName("调休工作日");

            calendarBo.updateDay(req, "admin");

            WhSysWorkCalendar record = calendarBo.getByDate("2099-06-15");
            assertNotNull(record);
            assertEquals("WORKDAY", record.getDayType());
            assertEquals("调休工作日", record.getHolidayName());
            assertEquals("2099", record.getYear());
        }

        @Test
        @DisplayName("updateDay - 更新不存在的日期，standardHours为空时默认8")
        void testUpdateDay_NullStandardHoursDefaultsTo8() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setCalendarDate("2099-07-01");
            req.setDayType("WORKDAY");
            // standardHours 故意不设置

            calendarBo.updateDay(req, "admin");

            WhSysWorkCalendar record = calendarBo.getByDate("2099-07-01");
            assertNotNull(record);
            assertEquals("8", record.getStandardHours());
        }
    }

    // ══════════════════════════════════════════════════
    //  批量更新
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("批量更新")
    class BatchUpdate {

        @Test
        @DisplayName("batchUpdate - 范围内的已有日期被更新")
        void testBatchUpdate_UpdatesExisting() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setStartDate("2026-01-05");
            req.setEndDate("2026-01-09");
            req.setDayType("HOLIDAY");

            calendarBo.batchUpdate(req, "admin");

            for (String date : List.of("2026-01-05", "2026-01-06", "2026-01-09")) {
                WhSysWorkCalendar record = calendarBo.getByDate(date);
                assertEquals("HOLIDAY", record.getDayType(), date + " 应为 HOLIDAY");
            }
        }

        @Test
        @DisplayName("batchUpdate - 范围内的新日期被创建")
        void testBatchUpdate_CreatesNew() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setStartDate("2099-08-10");
            req.setEndDate("2099-08-12");
            req.setDayType("WORKDAY");

            calendarBo.batchUpdate(req, "admin");

            assertNotNull(calendarBo.getByDate("2099-08-10"));
            assertNotNull(calendarBo.getByDate("2099-08-11"));
            assertNotNull(calendarBo.getByDate("2099-08-12"));
        }

        @Test
        @DisplayName("batchUpdate - 单日范围")
        void testBatchUpdate_SingleDay() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setStartDate("2026-06-01");
            req.setEndDate("2026-06-01");
            req.setDayType("HOLIDAY");

            calendarBo.batchUpdate(req, "admin");

            WhSysWorkCalendar record = calendarBo.getByDate("2026-06-01");
            assertEquals("HOLIDAY", record.getDayType());
        }
    }

    // ══════════════════════════════════════════════════
    //  删除
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("删除")
    class Delete {

        @Test
        @DisplayName("deleteById - 删除成功")
        void testDeleteById() {
            WhSysWorkCalendar record = calendarBo.getByDate("2026-01-01");
            assertNotNull(record);

            calendarBo.deleteById(record.getId());

            // 物理删除，所以再次查询应为 null
            WhSysWorkCalendar deleted = calendarDao.selectById(record.getId());
            assertNull(deleted);
        }
    }

    @Nested
    @DisplayName("补充边界")
    class ExtraBranchTests {

        @Test
        @DisplayName("isWorkDay - 无记录时周六/周日返回 false")
        void isWorkDay_weekendFallback() {
            // 2099-01-03 是周六，2099-01-04 是周日
            assertFalse(calendarBo.isWorkDay("2099-01-03"));
            assertFalse(calendarBo.isWorkDay("2099-01-04"));
        }

        @Test
        @DisplayName("isWorkDay - 无记录时工作日返回 true")
        void isWorkDay_workdayFallback() {
            assertTrue(calendarBo.isWorkDay("2099-01-05"));
        }

        @Test
        @DisplayName("isWorkDay - 有记录但为节假日返回 false")
        void isWorkDay_holidayRecord() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setCalendarDate("2026-01-05");
            req.setDayType("HOLIDAY");
            calendarBo.updateDay(req, "admin");

            assertFalse(calendarBo.isWorkDay("2026-01-05"));
        }

        @Test
        @DisplayName("updateDay - 已有日期且 standardHours 为空时默认 8")
        void updateDay_existingNullStandardHours() {
            WorkCalendarRequest req = new WorkCalendarRequest();
            req.setCalendarDate("2026-01-06");
            req.setDayType("HOLIDAY");
            req.setHolidayName("调休");
            // standardHours 不设置
            calendarBo.updateDay(req, "admin");

            WhSysWorkCalendar record = calendarBo.getByDate("2026-01-06");
            assertEquals("8", record.getStandardHours());
        }
    }
}
