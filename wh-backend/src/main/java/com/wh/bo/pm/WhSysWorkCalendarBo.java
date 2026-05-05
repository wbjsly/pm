package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhSysWorkCalendarDao;
import com.wh.entity.pm.WhSysWorkCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhSysWorkCalendarBo {

    private final WhSysWorkCalendarDao calendarDao;

    public WhSysWorkCalendarBo(WhSysWorkCalendarDao calendarDao) {
        this.calendarDao = calendarDao;
    }

    public List<WhSysWorkCalendar> getByYear(String year) {
        return calendarDao.selectByYear(year);
    }

    public WhSysWorkCalendar getByDate(String date) {
        return calendarDao.selectByDate(date);
    }

    @Transactional
    public void generateYear(String year, String createBy) {
        LocalDate date = LocalDate.of(Integer.parseInt(year), 1, 1);
        LocalDate end = LocalDate.of(Integer.parseInt(year), 12, 31);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<WhSysWorkCalendar> records = new ArrayList<>();
        while (!date.isAfter(end)) {
            WhSysWorkCalendar cal = new WhSysWorkCalendar();
            cal.setCalendarDate(date.format(fmt));
            cal.setYear(year);
            cal.setStandardHours("8");

            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                cal.setDayType("WEEKEND");
            } else {
                cal.setDayType("WORKDAY");
            }
            cal.setCreateBy(createBy);
            cal.setDelFlag("0");
            cal.setVerNo(0);
            records.add(cal);
            date = date.plusDays(1);
        }

        // Delete existing records for this year first
        LambdaQueryWrapper<WhSysWorkCalendar> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WhSysWorkCalendar::getYear, year);
        calendarDao.delete(deleteWrapper);

        // Batch insert
        for (WhSysWorkCalendar record : records) {
            calendarDao.insert(record);
        }
    }

    @Transactional
    public void updateDay(WorkCalendarRequest req, String updateBy) {
        LambdaQueryWrapper<WhSysWorkCalendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhSysWorkCalendar::getCalendarDate, req.getCalendarDate());

        WhSysWorkCalendar existing = calendarDao.selectOne(wrapper);
        if (existing != null) {
            existing.setDayType(req.getDayType());
            existing.setHolidayName(req.getHolidayName());
            existing.setStandardHours(req.getStandardHours() != null ? req.getStandardHours() : "8");
            existing.setUpdateBy(updateBy);
            calendarDao.updateById(existing);
        } else {
            WhSysWorkCalendar cal = new WhSysWorkCalendar();
            cal.setCalendarDate(req.getCalendarDate());
            cal.setDayType(req.getDayType());
            cal.setHolidayName(req.getHolidayName());
            cal.setStandardHours(req.getStandardHours() != null ? req.getStandardHours() : "8");
            cal.setYear(req.getCalendarDate().substring(0, 4));
            cal.setCreateBy(updateBy);
            cal.setDelFlag("0");
            cal.setVerNo(0);
            calendarDao.insert(cal);
        }
    }

    @Transactional
    public void batchUpdate(WorkCalendarRequest req, String updateBy) {
        LocalDate start = LocalDate.parse(req.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate end = LocalDate.parse(req.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate date = start;
        while (!date.isAfter(end)) {
            String dateStr = date.format(fmt);
            String year = dateStr.substring(0, 4);

            LambdaQueryWrapper<WhSysWorkCalendar> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WhSysWorkCalendar::getCalendarDate, dateStr);
            WhSysWorkCalendar existing = calendarDao.selectOne(wrapper);

            if (existing != null) {
                existing.setDayType(req.getDayType());
                existing.setUpdateBy(updateBy);
                calendarDao.updateById(existing);
            } else {
                WhSysWorkCalendar cal = new WhSysWorkCalendar();
                cal.setCalendarDate(dateStr);
                cal.setYear(year);
                cal.setDayType(req.getDayType());
                cal.setStandardHours("8");
                cal.setCreateBy(updateBy);
                cal.setDelFlag("0");
                cal.setVerNo(0);
                calendarDao.insert(cal);
            }
            date = date.plusDays(1);
        }
    }

    @Transactional
    public void deleteById(String id) {
        calendarDao.deleteById(id);
    }

    /**
     * Check if a date is a workday. Falls back to default (Mon-Fri = workday).
     */
    public boolean isWorkDay(String dateStr) {
        WhSysWorkCalendar record = calendarDao.selectByDate(dateStr);
        if (record != null) {
            return "WORKDAY".equals(record.getDayType());
        }
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /**
     * Get standard hours for a date. Defaults to 8.
     */
    public String getStandardHours(String dateStr) {
        WhSysWorkCalendar record = calendarDao.selectByDate(dateStr);
        if (record != null) {
            return record.getStandardHours();
        }
        return "8";
    }

    /**
     * Get all workday dates in a month range.
     */
    public List<String> getWorkDays(String year, int month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<String> workDays = new ArrayList<>();
        LocalDate date = start;
        while (!date.isAfter(end)) {
            String dateStr = date.format(fmt);
            if (isWorkDay(dateStr)) {
                workDays.add(dateStr);
            }
            date = date.plusDays(1);
        }
        return workDays;
    }

    /**
     * Get all days in a month with their calendar info.
     */
    public Map<String, WhSysWorkCalendar> getMonthCalendar(String year, int month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, WhSysWorkCalendar> result = new HashMap<>();
        LocalDate date = start;
        while (!date.isAfter(end)) {
            String dateStr = date.format(fmt);
            WhSysWorkCalendar record = calendarDao.selectByDate(dateStr);
            if (record != null) {
                result.put(dateStr, record);
            } else {
                // Use default
                WhSysWorkCalendar cal = new WhSysWorkCalendar();
                cal.setCalendarDate(dateStr);
                cal.setYear(year);
                cal.setStandardHours("8");
                DayOfWeek dow = date.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    cal.setDayType("WEEKEND");
                } else {
                    cal.setDayType("WORKDAY");
                }
                result.put(dateStr, cal);
            }
            date = date.plusDays(1);
        }
        return result;
    }
}
