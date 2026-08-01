package com.wh.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.ServiceException;
import com.wh.dao.sequence.WhSequenceDao;
import com.wh.entity.WhSequence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SequenceService 测试")
class SequenceServiceTest {

    @Mock
    private WhSequenceDao sequenceDao;

    @InjectMocks
    private SequenceService sequenceService;

    private WhSequence seq(int value, Integer year) {
        WhSequence s = new WhSequence();
        s.setSeqName("PM_CHARTER");
        s.setSeqValue(value);
        s.setSeqPrefix("CHARTER-");
        s.setSeqYear(year);
        return s;
    }

    @Nested
    @DisplayName("generateCode")
    class GenerateCode {

        @Test
        @DisplayName("序列不存在时抛出异常")
        void missingSequence_throws() {
            when(sequenceDao.selectOne(any())).thenReturn(null);
            assertThrows(ServiceException.class,
                    () -> sequenceService.generateCode("PM_CHARTER", "CHARTER-"));
        }

        @Test
        @DisplayName("同年递增生成编码")
        void sameYear_increments() {
            when(sequenceDao.selectOne(any())).thenReturn(seq(5, java.time.LocalDate.now().getYear()));
            String code = sequenceService.generateCode("PM_CHARTER", "CHARTER-");
            assertEquals("CHARTER-" + java.time.LocalDate.now().getYear() + "-006", code);
            verify(sequenceDao).incrementSeq("PM_CHARTER");
        }

        @Test
        @DisplayName("跨年重置从 001 开始")
        void newYear_resetsTo001() {
            when(sequenceDao.selectOne(any())).thenReturn(seq(42, 2025));
            String code = sequenceService.generateCode("PM_CHARTER", "CHARTER-");
            assertEquals("CHARTER-" + java.time.LocalDate.now().getYear() + "-001", code);
            verify(sequenceDao).updateSeqValue(eq("PM_CHARTER"), eq(1), eq(java.time.LocalDate.now().getYear()));
        }

        @Test
        @DisplayName("seqYear 为 null 时按新年重置")
        void nullYear_resetsTo001() {
            when(sequenceDao.selectOne(any())).thenReturn(seq(10, null));
            String code = sequenceService.generateCode("PM_CHARTER", "CHARTER-");
            assertEquals("CHARTER-" + java.time.LocalDate.now().getYear() + "-001", code);
        }
    }

    @Nested
    @DisplayName("getNextSequence")
    class GetNextSequence {

        @Test
        @DisplayName("序列不存在时创建并返回 1")
        void missingSequence_createsAndReturns1() {
            when(sequenceDao.selectOne(any())).thenReturn(null);
            long next = sequenceService.getNextSequence("WBS");
            assertEquals(1L, next);
            verify(sequenceDao).insert(any(WhSequence.class));
        }

        @Test
        @DisplayName("存在时递增并返回下一个值")
        void existing_increments() {
            WhSequence s = new WhSequence();
            s.setSeqName("WBS");
            s.setSeqValue(8);
            s.setSeqYear(java.time.LocalDate.now().getYear());
            when(sequenceDao.selectOne(any())).thenReturn(s);
            long next = sequenceService.getNextSequence("WBS");
            assertEquals(9L, next);
            verify(sequenceDao).incrementSeq("WBS");
        }
    }
}
