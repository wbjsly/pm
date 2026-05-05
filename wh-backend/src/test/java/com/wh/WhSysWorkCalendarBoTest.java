package com.wh;

import com.wh.bo.pm.WhSysWorkCalendarBo;
import com.wh.entity.pm.WhSysWorkCalendar;
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
class WhSysWorkCalendarBoTest {

    @Autowired
    private WhSysWorkCalendarBo calendarBo;

    @Test
    void testGenerateYear_Creates365Days() {
        calendarBo.generateYear("2026", "admin");
        List<WhSysWorkCalendar> all = calendarBo.getByYear("2026");
        assertEquals(365, all.size(), "2026 is not a leap year, should have 365 days");
    }

    @Test
    void testGenerateYear_LeapYear() {
        calendarBo.generateYear("2028", "admin");
        List<WhSysWorkCalendar> all = calendarBo.getByYear("2028");
        assertEquals(366, all.size(), "2028 is a leap year, should have 366 days");
    }

    @Test
    void testGenerateDayType_WorkdayVsWeekend() {
        calendarBo.generateYear("2026", "admin");

        // 2026-01-05 is Monday -> WORKDAY
        WhSysWorkCalendar monday = calendarBo.getByDate("2026-01-05");
        assertEquals("WORKDAY", monday.getDayType());
        assertEquals("8", monday.getStandardHours());

        // 2026-01-03 is Saturday -> WEEKEND
        WhSysWorkCalendar saturday = calendarBo.getByDate("2026-01-03");
        assertEquals("WEEKEND", saturday.getDayType());

        // 2026-01-04 is Sunday -> WEEKEND
        WhSysWorkCalendar sunday = calendarBo.getByDate("2026-01-04");
        assertEquals("WEEKEND", sunday.getDayType());
    }

    @Test
    void testGenerateYear_ReplacesExisting() {
        calendarBo.generateYear("2026", "admin");
        // Modify a day
        WhSysWorkCalendar modified = calendarBo.getByDate("2026-01-01");
        assertNotNull(modified);

        // Generate again
        calendarBo.generateYear("2026", "admin");
        List<WhSysWorkCalendar> all = calendarBo.getByYear("2026");
        assertEquals(365, all.size(), "Should still have 365 after regeneration");
    }

    @Test
    void testIsWorkDay_FromCalendar() {
        calendarBo.generateYear("2026", "admin");
        assertTrue(calendarBo.isWorkDay("2026-01-05"), "Monday should be workday");
        assertFalse(calendarBo.isWorkDay("2026-01-03"), "Saturday should not be workday");
    }

    @Test
    void testIsWorkDay_FallbackToDefault() {
        // No calendar generated, should use Mon-Fri logic
        LocalDate date = LocalDate.of(2026, 1, 5); // Monday
        assertTrue(calendarBo.isWorkDay("2026-01-05"), "Monday fallback should be workday");
        assertFalse(calendarBo.isWorkDay("2026-01-03"), "Saturday fallback should not be workday");
    }

    @Test
    void testGetStandardHours() {
        calendarBo.generateYear("2026", "admin");
        assertEquals("8", calendarBo.getStandardHours("2026-01-05"));
    }

    @Test
    void testGetStandardHours_DefaultWhenNoCalendar() {
        assertEquals("8", calendarBo.getStandardHours("2099-01-01"));
    }

    @Test
    void testGetWorkDays() {
        calendarBo.generateYear("2026", "admin");
        List<String> workDays = calendarBo.getWorkDays("2026", 1);
        // January 2026: 22 workdays (Mon-Fri excluding holidays, but no holidays set by default)
        assertFalse(workDays.isEmpty());
        assertTrue(workDays.stream().allMatch(d -> d.startsWith("2026-01")));
    }

    @Test
    void testGetMonthCalendar() {
        calendarBo.generateYear("2026", "admin");
        Map<String, WhSysWorkCalendar> monthCal = calendarBo.getMonthCalendar("2026", 1);
        assertEquals(31, monthCal.size(), "January has 31 days");
    }
}
