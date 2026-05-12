package com.wh.bo.system;

import com.wh.common.ServiceException;
import com.wh.constant.DictTypes;
import com.wh.dao.system.WhDictItemDao;
import com.wh.dao.system.WhDictTypeDao;
import com.wh.entity.system.WhDictItem;
import com.wh.entity.system.WhDictType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhPmDictBo {

    private static final Logger log = LoggerFactory.getLogger(WhPmDictBo.class);

    private final WhDictTypeDao dictTypeDao;
    private final WhDictItemDao dictItemDao;

    public WhPmDictBo(WhDictTypeDao dictTypeDao, WhDictItemDao dictItemDao) {
        this.dictTypeDao = dictTypeDao;
        this.dictItemDao = dictItemDao;
    }

    /**
     * 全量字典查询，按 type_code 分组，供前端一次性加载。
     */
    public Map<String, List<WhDictItem>> getAllDicts() {
        List<WhDictItem> items = dictItemDao.selectAllEnabled();
        return items.stream().collect(Collectors.groupingBy(WhDictItem::getTypeCode));
    }

    /**
     * 按类型获取启用的条目列表。
     */
    public List<WhDictItem> getItemsByType(String typeCode) {
        return dictItemDao.selectByTypeCode(typeCode);
    }

    /**
     * 获取所有字典类型。
     */
    public List<WhDictType> getAllTypes() {
        return dictTypeDao.selectList(null);
    }

    public WhDictType saveType(WhDictType type) {
        dictTypeDao.insert(type);
        return type;
    }

    public WhDictType updateType(WhDictType type) {
        dictTypeDao.updateById(type);
        return type;
    }

    public void deleteType(String id) {
        dictTypeDao.deleteById(id);
    }

    public WhDictItem saveItem(WhDictItem item) {
        dictItemDao.insert(item);
        return item;
    }

    public WhDictItem updateItem(WhDictItem item) {
        dictItemDao.updateById(item);
        return item;
    }

    public void deleteItem(String id) {
        dictItemDao.deleteById(id);
    }

    /**
     * 启动时校验 DictTypes 常量与字典表一致性。使用 ApplicationReadyEvent 确保在 SqliteBootstrap 建表之后执行。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateDictConsistency() {
        log.info("=== Dict Consistency Validation Start ===");
        try {
            List<WhDictType> types = dictTypeDao.selectList(null);
            Set<String> dbTypeCodes = types.stream().map(WhDictType::getTypeCode).collect(Collectors.toSet());

            List<String> allConstantTypeCodes = Arrays.asList(
                    DictTypes.CHARTER_STATUS,
                    DictTypes.BUDGET_STATUS,
                    DictTypes.DELIVERABLE_STATUS,
                    DictTypes.WBS_ELEMENT_STATUS,
                    DictTypes.WBS_ELEMENT_TYPE,
                    DictTypes.WORKLOG_STATUS,
                    DictTypes.COST_WARNING_STATUS,
                    DictTypes.CALENDAR_DAY_TYPE,
                    DictTypes.PRODUCT_STATUS,
                    DictTypes.MODULE_STATUS,
                    DictTypes.CHARTER_PROGRESS
            );

            for (String constCode : allConstantTypeCodes) {
                if (!dbTypeCodes.contains(constCode)) {
                    log.warn("Dict type_code constant '{}' not found in wh_dict_type table", constCode);
                }
            }
            for (String dbCode : dbTypeCodes) {
                if (!allConstantTypeCodes.contains(dbCode)) {
                    log.warn("wh_dict_type entry '{}' has no corresponding constant in DictTypes.java", dbCode);
                }
            }
            log.info("=== Dict Consistency Validation End ===");
        } catch (Exception e) {
            log.warn("Dict consistency validation skipped — tables may not exist yet: {}", e.getMessage());
        }
    }

    /**
     * 获取单个条目的 label，用于 Bo 类中获取中文名。
     */
    public String getLabel(String typeCode, String itemCode) {
        List<WhDictItem> items = getItemsByType(typeCode);
        return items.stream()
                .filter(i -> itemCode.equals(i.getItemCode()))
                .findFirst()
                .map(WhDictItem::getItemLabel)
                .orElse(itemCode);
    }
}
