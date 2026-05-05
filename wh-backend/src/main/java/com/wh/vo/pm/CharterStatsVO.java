package com.wh.vo.pm;

import lombok.Data;

@Data
public class CharterStatsVO {
    private int total;
    private int draft;
    private int pending;
    private int approved;
    private int rejected;
}
