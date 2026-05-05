package com.wh.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.dao.sequence.WhSequenceDao;
import com.wh.entity.WhSequence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SequenceService {

    private final WhSequenceDao sequenceDao;

    public SequenceService(WhSequenceDao sequenceDao) {
        this.sequenceDao = sequenceDao;
    }

    @Transactional
    public synchronized String generateCode(String seqName, String prefix) {
        int currentYear = java.time.LocalDate.now().getYear();

        WhSequence seq = sequenceDao.selectOne(
                new LambdaQueryWrapper<WhSequence>().eq(WhSequence::getSeqName, seqName)
        );

        if (seq == null) {
            throw new com.wh.common.ServiceException("序列不存在: " + seqName);
        }

        int value;
        if (seq.getSeqYear() == null || !seq.getSeqYear().equals(currentYear)) {
            value = 1;
            sequenceDao.updateSeqValue(seqName, value, currentYear);
        } else {
            sequenceDao.incrementSeq(seqName);
            value = seq.getSeqValue() + 1;
        }

        return prefix + currentYear + "-" + String.format("%03d", value);
    }

    /**
     * Get next sequence number as plain long (for WBS code generation).
     */
    @Transactional
    public synchronized long getNextSequence(String seqName) {
        WhSequence seq = sequenceDao.selectOne(
                new LambdaQueryWrapper<WhSequence>().eq(WhSequence::getSeqName, seqName)
        );

        if (seq == null) {
            // Create sequence on first use
            seq = new WhSequence();
            seq.setSeqName(seqName);
            seq.setSeqValue(0);
            seq.setSeqYear(java.time.LocalDate.now().getYear());
            sequenceDao.insert(seq);
            return 1;
        }

        sequenceDao.incrementSeq(seqName);
        return seq.getSeqValue() + 1;
    }
}
