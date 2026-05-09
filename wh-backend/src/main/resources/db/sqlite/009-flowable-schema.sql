-- Flowable 6.8.0 schema (H2 DDL converted to SQLite)
-- Auto-generated from Flowable H2 create scripts

create table ACT_ID_PROPERTY (
    NAME_ TEXT,
    VALUE_ TEXT,
    REV_ integer,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.8.0.0', 1);

create table ACT_ID_BYTEARRAY (
    ID_ TEXT,
    REV_ integer,
    NAME_ TEXT,
    BYTES_ BLOB,
    primary key (ID_)
);

create table ACT_ID_GROUP (
    ID_ TEXT,
    REV_ integer,
    NAME_ TEXT,
    TYPE_ TEXT,
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ TEXT,
    GROUP_ID_ TEXT,
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ TEXT,
    REV_ integer,
    FIRST_ TEXT,
    LAST_ TEXT,
    DISPLAY_NAME_ TEXT,
    EMAIL_ TEXT,
    PWD_ TEXT,
    PICTURE_ID_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ TEXT,
    REV_ integer,
    USER_ID_ TEXT,
    TYPE_ TEXT,
    KEY_ TEXT,
    VALUE_ TEXT,
    PASSWORD_ BLOB,
    PARENT_ID_ TEXT,
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ TEXT not null,
    REV_ integer,
    TOKEN_VALUE_ TEXT,
    TOKEN_DATE_ TEXT,
    IP_ADDRESS_ TEXT,
    USER_AGENT_ TEXT,
    USER_ID_ TEXT,
    TOKEN_DATA_ TEXT,
    primary key (ID_)
);

create table ACT_ID_PRIV (
    ID_ TEXT not null,
    NAME_ TEXT not null,
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ TEXT not null,
    PRIV_ID_ TEXT not null,
    USER_ID_ TEXT,
    GROUP_ID_ TEXT,
    primary key (ID_)
);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP;

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER;

alter table ACT_ID_PRIV_MAPPING
    add constraint ACT_FK_PRIV_MAPPING
    foreign key (PRIV_ID_)
    references ACT_ID_PRIV;

create index ACT_IDX_PRIV_USER on ACT_ID_PRIV_MAPPING(USER_ID_);

create index ACT_IDX_PRIV_GROUP on ACT_ID_PRIV_MAPPING(GROUP_ID_);

alter table ACT_ID_PRIV
    add constraint ACT_UNIQ_PRIV_NAME
    unique (NAME_);

create table ACT_HI_ENTITYLINK (
    ID_ TEXT,
    LINK_TYPE_ TEXT,
    CREATE_TIME_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    PARENT_ELEMENT_ID_ TEXT,
    REF_SCOPE_ID_ TEXT,
    REF_SCOPE_TYPE_ TEXT,
    REF_SCOPE_DEFINITION_ID_ TEXT,
    ROOT_SCOPE_ID_ TEXT,
    ROOT_SCOPE_TYPE_ TEXT,
    HIERARCHY_TYPE_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_HI_ENT_LNK_REF_SCOPE on ACT_HI_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_HI_ENT_LNK_ROOT_SCOPE on ACT_HI_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

create table ACT_RU_ENTITYLINK (
    ID_ TEXT,
    REV_ integer,
    CREATE_TIME_ TEXT,
    LINK_TYPE_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    PARENT_ELEMENT_ID_ TEXT,
    REF_SCOPE_ID_ TEXT,
    REF_SCOPE_TYPE_ TEXT,
    REF_SCOPE_DEFINITION_ID_ TEXT,
    ROOT_SCOPE_ID_ TEXT,
    ROOT_SCOPE_TYPE_ TEXT,
    HIERARCHY_TYPE_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_ENT_LNK_REF_SCOPE on ACT_RU_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

insert into ACT_GE_PROPERTY values ('entitylink.schema.version', '6.8.0.0', 1);

create table ACT_HI_VARINST (
    ID_ TEXT not null,
    REV_ integer default 1,
    PROC_INST_ID_ TEXT,
    EXECUTION_ID_ TEXT,
    TASK_ID_ TEXT,
    NAME_ TEXT not null,
    VAR_TYPE_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    BYTEARRAY_ID_ TEXT,
    DOUBLE_ REAL,
    LONG_ INTEGER,
    TEXT_ TEXT,
    TEXT2_ TEXT,
    CREATE_TIME_ TEXT,
    LAST_UPDATED_TIME_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);

create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);

create table ACT_RU_VARIABLE (
    ID_ TEXT not null,
    REV_ integer,
    TYPE_ TEXT not null,
    NAME_ TEXT not null,
    EXECUTION_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    TASK_ID_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    BYTEARRAY_ID_ TEXT,
    DOUBLE_ REAL,
    LONG_ INTEGER,
    TEXT_ TEXT,
    TEXT2_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_BYTEARRAY
    foreign key (BYTEARRAY_ID_)
    references ACT_GE_BYTEARRAY;

insert into ACT_GE_PROPERTY values ('variable.schema.version', '6.8.0.0', 1);

create table ACT_HI_IDENTITYLINK (
    ID_ TEXT,
    GROUP_ID_ TEXT,
    TYPE_ TEXT,
    USER_ID_ TEXT,
    TASK_ID_ TEXT,
    CREATE_TIME_ TEXT,
    PROC_INST_ID_ TEXT null,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);

create index ACT_IDX_HI_IDENT_LNK_SCOPE on ACT_HI_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_IDENT_LNK_SUB_SCOPE on ACT_HI_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_IDENT_LNK_SCOPE_DEF on ACT_HI_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create table ACT_RU_IDENTITYLINK (
    ID_ TEXT,
    REV_ integer,
    GROUP_ID_ TEXT,
    TYPE_ TEXT,
    USER_ID_ TEXT,
    TASK_ID_ TEXT,
    PROC_INST_ID_ TEXT null,
    PROC_DEF_ID_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);

create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);

create index ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('identitylink.schema.version', '6.8.0.0', 1);

create table ACT_GE_PROPERTY (
    NAME_ TEXT,
    VALUE_ TEXT,
    REV_ integer,
    primary key (NAME_)
);

create table ACT_GE_BYTEARRAY (
    ID_ TEXT,
    REV_ integer,
    NAME_ TEXT,
    DEPLOYMENT_ID_ TEXT,
    BYTES_ BLOB,
    GENERATED_ INTEGER,
    primary key (ID_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.8.0.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table FLW_RU_BATCH (
    ID_ TEXT not null,
    REV_ integer,
    TYPE_ TEXT not null,
    SEARCH_KEY_ TEXT,
    SEARCH_KEY2_ TEXT,
    CREATE_TIME_ TEXT not null,
    COMPLETE_TIME_ TEXT,
    STATUS_ TEXT,
    BATCH_DOC_ID_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table FLW_RU_BATCH_PART (
    ID_ TEXT not null,
    REV_ integer,
    BATCH_ID_ TEXT,
    TYPE_ TEXT not null,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SEARCH_KEY_ TEXT,
    SEARCH_KEY2_ TEXT,
    CREATE_TIME_ TEXT not null,
    COMPLETE_TIME_ TEXT,
    STATUS_ TEXT,
    RESULT_DOC_ID_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create index FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

alter table FLW_RU_BATCH_PART
    add constraint FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_);

insert into ACT_GE_PROPERTY values ('batch.schema.version', '6.8.0.0', 1);

create table ACT_HI_TASKINST (
    ID_ TEXT not null,
    REV_ integer default 1,
    PROC_DEF_ID_ TEXT,
    TASK_DEF_ID_ TEXT,
    TASK_DEF_KEY_ TEXT,
    PROC_INST_ID_ TEXT,
    EXECUTION_ID_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    PROPAGATED_STAGE_INST_ID_ TEXT,
    NAME_ TEXT,
    PARENT_TASK_ID_ TEXT,
    DESCRIPTION_ TEXT,
    OWNER_ TEXT,
    ASSIGNEE_ TEXT,
    START_TIME_ TEXT not null,
    CLAIM_TIME_ TEXT,
    END_TIME_ TEXT,
    DURATION_ INTEGER,
    DELETE_REASON_ TEXT,
    PRIORITY_ integer,
    DUE_DATE_ TEXT,
    FORM_KEY_ TEXT,
    CATEGORY_ TEXT,
    TENANT_ID_ TEXT default '',
    LAST_UPDATED_TIME_ TEXT,
    primary key (ID_)
);

create table ACT_HI_TSK_LOG (
    ID_ ,
    TYPE_ TEXT,
    TASK_ID_ TEXT not null,
    TIME_STAMP_ TEXT not null,
    USER_ID_ TEXT,
    DATA_ TEXT,
    EXECUTION_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    SCOPE_ID_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create table ACT_RU_TASK (
    ID_ TEXT,
    REV_ integer,
    EXECUTION_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    TASK_DEF_ID_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    PROPAGATED_STAGE_INST_ID_ TEXT,
    NAME_ TEXT,
    PARENT_TASK_ID_ TEXT,
    DESCRIPTION_ TEXT,
    TASK_DEF_KEY_ TEXT,
    OWNER_ TEXT,
    ASSIGNEE_ TEXT,
    DELEGATION_ TEXT,
    PRIORITY_ integer,
    CREATE_TIME_ TEXT,
    DUE_DATE_ TEXT,
    CATEGORY_ TEXT,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ TEXT default '',
    FORM_KEY_ TEXT,
    CLAIM_TIME_ TEXT,
    IS_COUNT_ENABLED_ INTEGER,
    VAR_COUNT_ integer, 
    ID_LINK_COUNT_ integer,
    SUB_TASK_COUNT_ integer,
    primary key (ID_)
);

create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);

create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '6.8.0.0', 1);

create table ACT_RU_JOB (
    ID_ TEXT NOT NULL,
    REV_ integer,
    CATEGORY_ TEXT,
    TYPE_ TEXT NOT NULL,
    LOCK_EXP_TIME_ TEXT,
    LOCK_OWNER_ TEXT,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ TEXT,
    PROCESS_INSTANCE_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    ELEMENT_ID_ TEXT,
    ELEMENT_NAME_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    CORRELATION_ID_ TEXT,
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ TEXT,
    EXCEPTION_MSG_ TEXT,
    DUEDATE_ TEXT,
    REPEAT_ TEXT,
    HANDLER_TYPE_ TEXT,
    HANDLER_CFG_ TEXT,
    CUSTOM_VALUES_ID_ TEXT,
    CREATE_TIME_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_RU_TIMER_JOB (
    ID_ TEXT NOT NULL,
    REV_ integer,
    CATEGORY_ TEXT,
    TYPE_ TEXT NOT NULL,
    LOCK_EXP_TIME_ TEXT,
    LOCK_OWNER_ TEXT,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ TEXT,
    PROCESS_INSTANCE_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    ELEMENT_ID_ TEXT,
    ELEMENT_NAME_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    CORRELATION_ID_ TEXT,
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ TEXT,
    EXCEPTION_MSG_ TEXT,
    DUEDATE_ TEXT,
    REPEAT_ TEXT,
    HANDLER_TYPE_ TEXT,
    HANDLER_CFG_ TEXT,
    CUSTOM_VALUES_ID_ TEXT,
    CREATE_TIME_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_RU_SUSPENDED_JOB (
    ID_ TEXT NOT NULL,
    REV_ integer,
    CATEGORY_ TEXT,
    TYPE_ TEXT NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ TEXT,
    PROCESS_INSTANCE_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    ELEMENT_ID_ TEXT,
    ELEMENT_NAME_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    CORRELATION_ID_ TEXT,
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ TEXT,
    EXCEPTION_MSG_ TEXT,
    DUEDATE_ TEXT,
    REPEAT_ TEXT,
    HANDLER_TYPE_ TEXT,
    HANDLER_CFG_ TEXT,
    CUSTOM_VALUES_ID_ TEXT,
    CREATE_TIME_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_RU_DEADLETTER_JOB (
    ID_ TEXT NOT NULL,
    REV_ integer,
    CATEGORY_ TEXT,
    TYPE_ TEXT NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ TEXT,
    PROCESS_INSTANCE_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    ELEMENT_ID_ TEXT,
    ELEMENT_NAME_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    CORRELATION_ID_ TEXT,
    EXCEPTION_STACK_ID_ TEXT,
    EXCEPTION_MSG_ TEXT,
    DUEDATE_ TEXT,
    REPEAT_ TEXT,
    HANDLER_TYPE_ TEXT,
    HANDLER_CFG_ TEXT,
    CUSTOM_VALUES_ID_ TEXT,
    CREATE_TIME_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_RU_HISTORY_JOB (
    ID_ TEXT NOT NULL,
    REV_ integer,
    LOCK_EXP_TIME_ TEXT,
    LOCK_OWNER_ TEXT,
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ TEXT,
    EXCEPTION_MSG_ TEXT,
    HANDLER_TYPE_ TEXT,
    HANDLER_CFG_ TEXT,
    CUSTOM_VALUES_ID_ TEXT,
    ADV_HANDLER_CFG_ID_ TEXT,
    CREATE_TIME_ TEXT,
    SCOPE_TYPE_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_RU_EXTERNAL_JOB (
    ID_ TEXT NOT NULL,
    REV_ integer,
    CATEGORY_ TEXT,
    TYPE_ TEXT NOT NULL,
    LOCK_EXP_TIME_ TEXT,
    LOCK_OWNER_ TEXT,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ TEXT,
    PROCESS_INSTANCE_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    ELEMENT_ID_ TEXT,
    ELEMENT_NAME_ TEXT,
    SCOPE_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    CORRELATION_ID_ TEXT,
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ TEXT,
    EXCEPTION_MSG_ TEXT,
    DUEDATE_ TEXT,
    REPEAT_ TEXT,
    HANDLER_TYPE_ TEXT,
    HANDLER_CFG_ TEXT,
    CUSTOM_VALUES_ID_ TEXT,
    CREATE_TIME_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create index ACT_IDX_JOB_EXCEPTION_STACK_ID on ACT_RU_JOB(EXCEPTION_STACK_ID_);

create index ACT_IDX_JOB_CUSTOM_VALUES_ID on ACT_RU_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_JOB_CORRELATION_ID on ACT_RU_JOB(CORRELATION_ID_);

create index ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID on ACT_RU_TIMER_JOB(EXCEPTION_STACK_ID_);

create index ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID on ACT_RU_TIMER_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_TIMER_JOB_CORRELATION_ID on ACT_RU_TIMER_JOB(CORRELATION_ID_);

create index ACT_IDX_TIMER_JOB_DUEDATE on ACT_RU_TIMER_JOB(DUEDATE_);

create index ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID on ACT_RU_SUSPENDED_JOB(EXCEPTION_STACK_ID_);

create index ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID on ACT_RU_SUSPENDED_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_SUSPENDED_JOB_CORRELATION_ID on ACT_RU_SUSPENDED_JOB(CORRELATION_ID_);

create index ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID on ACT_RU_DEADLETTER_JOB(EXCEPTION_STACK_ID_);

create index ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID on ACT_RU_DEADLETTER_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_DEADLETTER_JOB_CORRELATION_ID on ACT_RU_DEADLETTER_JOB(CORRELATION_ID_);

create index ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID on ACT_RU_EXTERNAL_JOB(EXCEPTION_STACK_ID_);

create index ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID on ACT_RU_EXTERNAL_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_EXTERNAL_JOB_CORRELATION_ID on ACT_RU_EXTERNAL_JOB(CORRELATION_ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_JOB_SCOPE on ACT_RU_JOB(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_JOB_SUB_SCOPE on ACT_RU_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_JOB_SCOPE_DEF on ACT_RU_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SCOPE on ACT_RU_TIMER_JOB(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SUB_SCOPE on ACT_RU_TIMER_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SCOPE_DEF on ACT_RU_TIMER_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_SJOB_SCOPE on ACT_RU_SUSPENDED_JOB(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_SJOB_SUB_SCOPE on ACT_RU_SUSPENDED_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_SJOB_SCOPE_DEF on ACT_RU_SUSPENDED_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_DJOB_SCOPE on ACT_RU_DEADLETTER_JOB(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_DJOB_SUB_SCOPE on ACT_RU_DEADLETTER_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_DJOB_SCOPE_DEF on ACT_RU_DEADLETTER_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_EJOB_SCOPE on ACT_RU_EXTERNAL_JOB(SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_EJOB_SUB_SCOPE on ACT_RU_EXTERNAL_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_EJOB_SCOPE_DEF on ACT_RU_EXTERNAL_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('job.schema.version', '6.8.0.0', 1);

create table ACT_RE_DEPLOYMENT (
    ID_ TEXT,
    NAME_ TEXT,
    CATEGORY_ TEXT,
    KEY_ TEXT,
    TENANT_ID_ TEXT default '',
    DEPLOY_TIME_ TEXT,
    DERIVED_FROM_ TEXT,
    DERIVED_FROM_ROOT_ TEXT,
    PARENT_DEPLOYMENT_ID_ TEXT,
    ENGINE_VERSION_ TEXT,
    primary key (ID_)
);

create table ACT_RE_MODEL (
    ID_ TEXT not null,
    REV_ integer,
    NAME_ TEXT,
    KEY_ TEXT,
    CATEGORY_ TEXT,
    CREATE_TIME_ TEXT,
    LAST_UPDATE_TIME_ TEXT,
    VERSION_ integer,
    META_INFO_ TEXT,
    DEPLOYMENT_ID_ TEXT,
    EDITOR_SOURCE_VALUE_ID_ TEXT,
    EDITOR_SOURCE_EXTRA_VALUE_ID_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_RU_EXECUTION (
    ID_ TEXT,
    REV_ integer,
    PROC_INST_ID_ TEXT,
    BUSINESS_KEY_ TEXT,
    PARENT_ID_ TEXT,
    PROC_DEF_ID_ TEXT,
    SUPER_EXEC_ TEXT,
    ROOT_PROC_INST_ID_ TEXT,
    ACT_ID_ TEXT,
    IS_ACTIVE_ INTEGER,
    IS_CONCURRENT_ INTEGER,
    IS_SCOPE_ INTEGER,
    IS_EVENT_SCOPE_ INTEGER,
    IS_MI_ROOT_ INTEGER,
    SUSPENSION_STATE_ integer,
    CACHED_ENT_STATE_ integer,
    TENANT_ID_ TEXT default '',
    NAME_ TEXT,
    START_ACT_ID_ TEXT,
    START_TIME_ TEXT,
    START_USER_ID_ TEXT,
    LOCK_TIME_ TEXT,
    LOCK_OWNER_ TEXT,
    IS_COUNT_ENABLED_ INTEGER,
    EVT_SUBSCR_COUNT_ integer, 
    TASK_COUNT_ integer, 
    JOB_COUNT_ integer, 
    TIMER_JOB_COUNT_ integer,
    SUSP_JOB_COUNT_ integer,
    DEADLETTER_JOB_COUNT_ integer,
    EXTERNAL_WORKER_JOB_COUNT_ integer,
    VAR_COUNT_ integer, 
    ID_LINK_COUNT_ integer,
    CALLBACK_ID_ TEXT,
    CALLBACK_TYPE_ TEXT,
    REFERENCE_ID_ TEXT,
    REFERENCE_TYPE_ TEXT,
    PROPAGATED_STAGE_INST_ID_ TEXT,
    BUSINESS_STATUS_ TEXT,
    primary key (ID_)
);

create table ACT_RE_PROCDEF (
    ID_ TEXT NOT NULL,
    REV_ integer,
    CATEGORY_ TEXT,
    NAME_ TEXT,
    KEY_ TEXT NOT NULL,
    VERSION_ integer NOT NULL,
    DEPLOYMENT_ID_ TEXT,
    RESOURCE_NAME_ TEXT,
    DGRM_RESOURCE_NAME_ TEXT,
    DESCRIPTION_ TEXT,
    HAS_START_FORM_KEY_ INTEGER,
    HAS_GRAPHICAL_NOTATION_ INTEGER,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ TEXT default '',
    DERIVED_FROM_ TEXT,
    DERIVED_FROM_ROOT_ TEXT,
    DERIVED_VERSION_ integer NOT NULL default 0,
    ENGINE_VERSION_ TEXT,
    primary key (ID_)
);

create table ACT_EVT_LOG (
    LOG_NR_ ,
    TYPE_ TEXT,
    PROC_DEF_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    EXECUTION_ID_ TEXT,
    TASK_ID_ TEXT,
    TIME_STAMP_ TEXT not null,
    USER_ID_ TEXT,
    DATA_ BLOB,
    LOCK_OWNER_ TEXT,
    LOCK_TIME_ TEXT,
    IS_PROCESSED_ INTEGER default 0
);

create table ACT_PROCDEF_INFO (
	ID_ TEXT not null,
    PROC_DEF_ID_ TEXT not null,
    REV_ integer,
    INFO_JSON_ID_ TEXT,
    primary key (ID_)
);

create table ACT_RU_ACTINST (
    ID_ TEXT not null,
    REV_ integer default 1,
    PROC_DEF_ID_ TEXT not null,
    PROC_INST_ID_ TEXT not null,
    EXECUTION_ID_ TEXT not null,
    ACT_ID_ TEXT not null,
    TASK_ID_ TEXT,
    CALL_PROC_INST_ID_ TEXT,
    ACT_NAME_ TEXT,
    ACT_TYPE_ TEXT not null,
    ASSIGNEE_ TEXT,
    START_TIME_ TEXT not null,
    END_TIME_ TEXT,
    TRANSACTION_ORDER_ integer,
    DURATION_ INTEGER,
    DELETE_REASON_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);

create index ACT_IDC_EXEC_ROOT on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);

create index ACT_IDX_EXEC_REF_ID_ on ACT_RU_EXECUTION(REFERENCE_ID_);

create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);

create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);

create index ACT_IDX_INFO_PROCDEF on ACT_PROCDEF_INFO(PROC_DEF_ID_);

create index ACT_IDX_RU_ACTI_START on ACT_RU_ACTINST(START_TIME_);

create index ACT_IDX_RU_ACTI_END on ACT_RU_ACTINST(END_TIME_);

create index ACT_IDX_RU_ACTI_PROC on ACT_RU_ACTINST(PROC_INST_ID_);

create index ACT_IDX_RU_ACTI_PROC_ACT on ACT_RU_ACTINST(PROC_INST_ID_, ACT_ID_);

create index ACT_IDX_RU_ACTI_EXEC on ACT_RU_ACTINST(EXECUTION_ID_);

create index ACT_IDX_RU_ACTI_EXEC_ACT on ACT_RU_ACTINST(EXECUTION_ID_, ACT_ID_);

create index ACT_IDX_RU_ACTI_TASK on ACT_RU_ACTINST(TASK_ID_);

alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT;

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, DERIVED_VERSION_, TENANT_ID_);

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT
    foreign key (PARENT_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER 
    foreign key (SUPER_EXEC_) 
    references ACT_RU_EXECUTION;

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK
    foreign key (TASK_ID_)
    references ACT_RU_TASK;

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_ATHRZ_PROCEDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF;

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_IDL_PROCINST
    foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_TASK
  add constraint ACT_FK_TASK_PROCDEF
  foreign key (PROC_DEF_ID_)
  references ACT_RE_PROCDEF;

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXECUTION
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_PROCESS_INSTANCE
    foreign key (PROCESS_INSTANCE_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF;

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_EXECUTION
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_PROCESS_INSTANCE
    foreign key (PROCESS_INSTANCE_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF;

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_EXECUTION
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE
    foreign key (PROCESS_INSTANCE_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF;

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_EXECUTION
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE
    foreign key (PROCESS_INSTANCE_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF;

alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION;

alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_SOURCE 
    foreign key (EDITOR_SOURCE_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_SOURCE_EXTRA 
    foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_DEPLOYMENT 
    foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_PROCDEF_INFO 
    add constraint ACT_FK_INFO_JSON_BA 
    foreign key (INFO_JSON_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_PROCDEF_INFO 
    add constraint ACT_FK_INFO_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);

alter table ACT_PROCDEF_INFO
    add constraint ACT_UNIQ_INFO_PROCDEF
    unique (PROC_DEF_ID_);

insert into ACT_GE_PROPERTY
values ('schema.version', '6.8.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(6.8.0.0)', 1);

create table ACT_HI_PROCINST (
    ID_ TEXT not null,
    REV_ integer default 1,
    PROC_INST_ID_ TEXT not null,
    BUSINESS_KEY_ TEXT,
    PROC_DEF_ID_ TEXT not null,
    START_TIME_ TEXT not null,
    END_TIME_ TEXT,
    DURATION_ INTEGER,
    START_USER_ID_ TEXT,
    START_ACT_ID_ TEXT,
    END_ACT_ID_ TEXT,
    SUPER_PROCESS_INSTANCE_ID_ TEXT,
    DELETE_REASON_ TEXT,
    TENANT_ID_ TEXT default '',
    NAME_ TEXT,
    CALLBACK_ID_ TEXT,
    CALLBACK_TYPE_ TEXT,
    REFERENCE_ID_ TEXT,
    REFERENCE_TYPE_ TEXT,
    PROPAGATED_STAGE_INST_ID_ TEXT,
    BUSINESS_STATUS_ TEXT,
    primary key (ID_),
    unique (PROC_INST_ID_)
);

create table ACT_HI_ACTINST (
    ID_ TEXT not null,
    REV_ integer default 1,
    PROC_DEF_ID_ TEXT not null,
    PROC_INST_ID_ TEXT not null,
    EXECUTION_ID_ TEXT not null,
    ACT_ID_ TEXT not null,
    TASK_ID_ TEXT,
    CALL_PROC_INST_ID_ TEXT,
    ACT_NAME_ TEXT,
    ACT_TYPE_ TEXT not null,
    ASSIGNEE_ TEXT,
    START_TIME_ TEXT not null,
    END_TIME_ TEXT,
    TRANSACTION_ORDER_ integer,
    DURATION_ INTEGER,
    DELETE_REASON_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create table ACT_HI_DETAIL (
    ID_ TEXT not null,
    TYPE_ TEXT not null,
    TIME_ TEXT not null,
    NAME_ TEXT,
    PROC_INST_ID_ TEXT,
    EXECUTION_ID_ TEXT,
    TASK_ID_ TEXT,
    ACT_INST_ID_ TEXT,
    VAR_TYPE_ TEXT,
    REV_ integer,
    BYTEARRAY_ID_ TEXT,
    DOUBLE_ REAL,
    LONG_ INTEGER,
    TEXT_ TEXT,
    TEXT2_ TEXT,
    primary key (ID_)
);

create table ACT_HI_COMMENT (
    ID_ TEXT not null,
    TYPE_ TEXT,
    TIME_ TEXT not null,
    USER_ID_ TEXT,
    TASK_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    ACTION_ TEXT,
    MESSAGE_ TEXT,
    FULL_MSG_ BLOB,
    primary key (ID_)
);

create table ACT_HI_ATTACHMENT (
    ID_ TEXT not null,
    REV_ integer,
    USER_ID_ TEXT,
    NAME_ TEXT,
    DESCRIPTION_ TEXT,
    TYPE_ TEXT,
    TASK_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    URL_ TEXT,
    CONTENT_ID_ TEXT,
    TIME_ TEXT,
    primary key (ID_)
);

create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);

create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);

create index ACT_IDX_HI_PRO_SUPER_PROCINST on ACT_HI_PROCINST(SUPER_PROCESS_INSTANCE_ID_);

create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_);

create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_);

create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_);

create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_);

create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_);

create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_);

create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_);

create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_);

create index ACT_IDX_HI_PROCVAR_TASK_ID on ACT_HI_VARINST(TASK_ID_);

create index ACT_IDX_HI_PROCVAR_EXE on ACT_HI_VARINST(EXECUTION_ID_);

create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);

create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);

create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);

create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);

create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

create table ACT_RU_EVENT_SUBSCR (
    ID_ TEXT not null,
    REV_ integer,
    EVENT_TYPE_ TEXT not null,
    EVENT_NAME_ TEXT,
    EXECUTION_ID_ TEXT,
    PROC_INST_ID_ TEXT,
    ACTIVITY_ID_ TEXT,
    CONFIGURATION_ TEXT,
    CREATED_ TEXT not null,
    PROC_DEF_ID_ TEXT,
    SUB_SCOPE_ID_ TEXT,
    SCOPE_ID_ TEXT,
    SCOPE_DEFINITION_ID_ TEXT,
    SCOPE_TYPE_ TEXT,
    LOCK_TIME_ TEXT,
    LOCK_OWNER_ TEXT,
    TENANT_ID_ TEXT default '',
    primary key (ID_)
);

create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);

create index ACT_IDX_EVENT_SUBSCR_SCOPEREF_ on ACT_RU_EVENT_SUBSCR(SCOPE_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('eventsubscription.schema.version', '6.8.0.0', 1);