-- Grant admin user work hour permission for existing projects
-- by adding them as WBS element owners

-- 供应链数字化平台 (charter0000000000000000000000003)
INSERT OR IGNORE INTO pm_wbs_element (
    ID, PROJECT_ID, WBS_CODE, PARENT_ID, LEVEL,
    NAME, DESCRIPTION, ELEMENT_TYPE, OWNER_ID, PLANNED_OWNER_ID,
    STATUS, SORT_ORDER, CREATE_BY, DEL_FLAG, VER_NO
) VALUES (
    'wbs_admin_supplychain_001',
    'charter0000000000000000000000003',
    'ADM-SC-001',
    NULL,
    1,
    '系统管理',
    '系统管理员工时录入条目',
    'TASK',
    'user00000000000000000000000000001',
    'user00000000000000000000000000001',
    'PLANNED',
    0,
    'system',
    '0',
    0
);
