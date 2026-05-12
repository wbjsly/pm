package com.wh.dao.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.system.WhDictItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WhDictItemDao extends BaseMapper<WhDictItem> {

    default List<WhDictItem> selectByTypeCode(String typeCode) {
        LambdaQueryWrapper<WhDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhDictItem::getTypeCode, typeCode)
               .eq(WhDictItem::getStatus, "1")
               .orderByAsc(WhDictItem::getSortOrder);
        return selectList(wrapper);
    }

    default List<WhDictItem> selectAllEnabled() {
        LambdaQueryWrapper<WhDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhDictItem::getStatus, "1")
               .orderByAsc(WhDictItem::getSortOrder);
        return selectList(wrapper);
    }
}
