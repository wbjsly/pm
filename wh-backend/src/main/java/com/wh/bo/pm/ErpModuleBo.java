package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.ServiceException;
import com.wh.dao.pm.ErpModuleDao;
import com.wh.entity.pm.ErpModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErpModuleBo {

    private final ErpModuleDao moduleDao;

    public ErpModuleBo(ErpModuleDao moduleDao) {
        this.moduleDao = moduleDao;
    }

    public IPage<ErpModule> pageList(int pageNum, int pageSize, String productId, String keyword) {
        Page<ErpModule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ErpModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpModule::getDelFlag, "0");
        if (productId != null && !productId.isEmpty()) {
            wrapper.eq(ErpModule::getProductId, productId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            // 必须用 and(...) 嵌套，否则 OR 会绕过 del_flag 等前置条件
            wrapper.and(w -> w.like(ErpModule::getModuleCode, keyword)
                    .or()
                    .like(ErpModule::getModuleName, keyword));
        }
        wrapper.orderByDesc(ErpModule::getCreateDate);
        return moduleDao.selectPage(page, wrapper);
    }

    public List<ErpModule> listByProductId(String productId) {
        LambdaQueryWrapper<ErpModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpModule::getProductId, productId);
        wrapper.eq(ErpModule::getDelFlag, "0");
        return moduleDao.selectList(wrapper);
    }

    public ErpModule getById(String id) {
        ErpModule module = moduleDao.selectById(id);
        if (module == null || "1".equals(module.getDelFlag())) {
            throw new ServiceException(404, "模块不存在");
        }
        return module;
    }

    @Transactional
    public ErpModule create(ErpModule module) {
        module.setDelFlag("0");
        module.setVerNo(0);
        if (module.getStatus() == null || module.getStatus().isEmpty()) {
            module.setStatus("ACTIVE");
        }
        moduleDao.insert(module);
        return module;
    }

    @Transactional
    public void update(String id, ErpModule req) {
        ErpModule module = getById(id);
        module.setModuleName(req.getModuleName());
        module.setModuleCode(req.getModuleCode());
        module.setDescription(req.getDescription());
        module.setStatus(req.getStatus());
        module.setModuleVersion(req.getModuleVersion());
        module.setVersionReleaseDate(req.getVersionReleaseDate());
        module.setRemarks(req.getRemarks());
        moduleDao.updateById(module);
    }

    @Transactional
    public void delete(String id) {
        getById(id);
        moduleDao.deleteById(id);
    }
}
