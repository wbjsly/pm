package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmWbsElement;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmWbsElementDao extends BaseMapper<WhPmWbsElement> {

    @Delete("DELETE FROM pm_wbs_element WHERE ID = #{id}")
    int physicalDeleteById(String id);

    @Select("SELECT * FROM pm_wbs_element WHERE PARENT_ID = #{parentId} AND DEL_FLAG = '0' ORDER BY SORT_ORDER")
    List<WhPmWbsElement> selectChildrenByParentId(String parentId);

    @Select("SELECT COUNT(*) FROM pm_wbs_element WHERE PARENT_ID = #{parentId} AND DEL_FLAG = '0'")
    int countChildren(String parentId);

    @Select("SELECT * FROM pm_wbs_element WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' ORDER BY SORT_ORDER")
    List<WhPmWbsElement> selectByProjectId(String projectId);

    @Select("SELECT * FROM pm_wbs_element WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' " +
            "AND (NAME LIKE '%' || #{keyword} || '%' OR WBS_CODE LIKE '%' || #{keyword} || '%') " +
            "ORDER BY SORT_ORDER")
    List<WhPmWbsElement> selectByProjectIdWithKeyword(@Param("projectId") String projectId,
                                                      @Param("keyword") String keyword);

    @Select("SELECT * FROM pm_wbs_element WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' " +
            "AND STATUS = #{status} ORDER BY SORT_ORDER")
    List<WhPmWbsElement> selectByProjectIdAndStatus(@Param("projectId") String projectId,
                                                    @Param("status") String status);
}
