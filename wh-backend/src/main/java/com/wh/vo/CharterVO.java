package com.wh.vo;

import com.wh.entity.pm.WhPmCharter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CharterVO extends WhPmCharter {
    private String createByName;
}
