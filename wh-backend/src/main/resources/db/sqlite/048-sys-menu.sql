-- 菜单表 DDL + 种子数据

CREATE TABLE IF NOT EXISTS sys_menu (
    ID TEXT NOT NULL,
    PARENT_ID TEXT DEFAULT '',
    TITLE TEXT NOT NULL,
    PATH TEXT NOT NULL,
    COMPONENT TEXT,
    ICON TEXT DEFAULT '',
    SORT_ORDER INTEGER DEFAULT 0,
    PERM TEXT DEFAULT '',
    STATUS TEXT DEFAULT '1',
    CREATE_BY TEXT,
    CREATE_DATE TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY TEXT,
    UPDATE_DATE TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS TEXT,
    DEL_FLAG TEXT DEFAULT '0',
    VER_NO INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE TEXT,
    CONSTRAINT PK_SYS_MENU PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_SYS_MENU_STATUS ON sys_menu(STATUS);

-- 一级菜单（parent_id 为空，sort_order 控制显示顺序）
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU_HOME', '', '主页', '/dashboard', 'HomeFilled', 1, '', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU_PM', '', '项目管理', '', 'Folder', 2, 'ROLE_PM', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU_SYS', '', '系统管理', '', 'Setting', 3, 'ROLE_ADMIN', '1');

-- 项目管理子菜单
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU001', 'MENU_PM', '项目立项', '/pm/charter', 'Document', 1, 'ROLE_PM', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU002', 'MENU_PM', '项目任务', '/pm/wbs', 'List', 2, 'ROLE_PM', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU003', 'MENU_PM', '预算管理', '/pm/budget', 'Money', 3, 'ROLE_PM', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU004', 'MENU_PM', '工时管理', '/pm/work-hours', 'Clock', 4, 'ROLE_PM', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU005', 'MENU_PM', '成果管理', '/pm/deliverable', 'Folder', 5, 'ROLE_PM,ROLE_SPONSOR', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU006', 'MENU_PM', '产品清单', '/pm/product', 'Tickets', 6, 'ROLE_PM', '1');


-- 系统管理子菜单
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU007', 'MENU_SYS', '用户管理', '/system/user', 'User', 2, 'ROLE_ADMIN', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU008', 'MENU_SYS', '工作日历', '/system/calendar', 'Calendar', 3, 'ROLE_ADMIN', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU009', 'MENU_SYS', '成本定额', '/system/cost-quota', 'Money', 4, 'ROLE_ADMIN,ROLE_PM', '1');
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS) VALUES ('MENU010', 'MENU_SYS', '菜单管理', '/system/menu', 'Menu', 5, 'ROLE_ADMIN', '1');
