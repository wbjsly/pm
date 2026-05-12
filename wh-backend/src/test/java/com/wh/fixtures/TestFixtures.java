package com.wh.fixtures;

import com.wh.bo.pm.*;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestFixtures {

    @Autowired
    private WhPmCharterBo charterBo;

    @Autowired
    private WhPmWorkLogBo workLogBo;

    @Autowired
    private WhSysWorkCalendarBo calendarBo;

    public static final String PM_USER = "pm_001";
    public static final String REGULAR_USER = "user_001";

    public WhPmCharter createTestProject(String name) {
        calendarBo.generateYear("2026", "system");
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName(name);
        req.setProjectCode("TEST-" + System.currentTimeMillis());
        req.setProjectShortName("T");
        req.setSponsorId(PM_USER);
        req.setPmId(PM_USER);
        return charterBo.create(req);
    }

    public WhPmWorkLog createDraftWorkLog(String projectId, String date, String hours) {
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate(date);
        req.setHoursWorked(hours);
        req.setWorkDescription("测试工时");
        return workLogBo.create(PM_USER, req);
    }

    public WhPmCharter createApprovedProject(String name) {
        WhPmCharter charter = createTestProject(name);
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "测试审批通过");
        return charterBo.getById(charter.getId());
    }
}
