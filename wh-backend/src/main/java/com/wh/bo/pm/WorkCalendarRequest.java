package com.wh.bo.pm;

import lombok.Data;

@Data
public class WorkCalendarRequest {
    private String year;
    private String startDate;
    private String endDate;
    private String calendarDate;
    private String dayType;
    private String holidayName;
    private String standardHours;
}
