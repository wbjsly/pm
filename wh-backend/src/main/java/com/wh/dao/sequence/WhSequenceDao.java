package com.wh.dao.sequence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.WhSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WhSequenceDao extends BaseMapper<WhSequence> {

    @Select("SELECT SEQ_VALUE FROM wh_sequence WHERE SEQ_NAME = #{seqName}")
    Integer getSeqValue(@Param("seqName") String seqName);

    @Update("UPDATE wh_sequence SET SEQ_VALUE = #{seqValue}, SEQ_YEAR = #{seqYear} WHERE SEQ_NAME = #{seqName}")
    int updateSeqValue(@Param("seqName") String seqName, @Param("seqValue") int seqValue, @Param("seqYear") int seqYear);

    @Update("UPDATE wh_sequence SET SEQ_VALUE = SEQ_VALUE + 1 WHERE SEQ_NAME = #{seqName}")
    int incrementSeq(@Param("seqName") String seqName);
}
