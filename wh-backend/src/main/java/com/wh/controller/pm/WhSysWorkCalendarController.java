package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.bo.pm.WhSysWorkCalendarBo;
import com.wh.bo.pm.WorkCalendarRequest;
import com.wh.common.R;
import com.wh.entity.pm.WhSysWorkCalendar;
import com.wh.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/work-calendar")
public class WhSysWorkCalendarController {

    private final WhSysWorkCalendarBo calendarBo;

    public WhSysWorkCalendarController(WhSysWorkCalendarBo calendarBo) {
        this.calendarBo = calendarBo;
    }

    @GetMapping
    public R<List<WhSysWorkCalendar>> getByYear(@RequestParam String year) {
        return R.ok(calendarBo.getByYear(year));
    }

    @GetMapping("/month")
    public R<Map<String, WhSysWorkCalendar>> getByMonth(@RequestParam String year, @RequestParam int month) {
        return R.ok(calendarBo.getMonthCalendar(year, month));
    }

    @GetMapping("/{id}")
    public R<WhSysWorkCalendar> getById(@PathVariable String id) {
        return R.ok(calendarBo.getByDate(id));
    }

    @PostMapping
    public R<Void> updateDay(@RequestBody WorkCalendarRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        calendarBo.updateDay(req, userId);
        return R.ok();
    }

    @PostMapping("/batch")
    public R<Void> batchUpdate(@RequestBody WorkCalendarRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        calendarBo.batchUpdate(req, userId);
        return R.ok();
    }

    @PostMapping("/generate")
    public R<Void> generateYear(@RequestBody Map<String, String> params) {
        String year = params.get("year");
        String userId = SecurityUtils.getCurrentUserId();
        calendarBo.generateYear(year, userId);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        calendarBo.deleteById(id);
        return R.ok();
    }

    @GetMapping("/is-workday")
    public R<Boolean> isWorkDay(@RequestParam String date) {
        return R.ok(calendarBo.isWorkDay(date));
    }
}
