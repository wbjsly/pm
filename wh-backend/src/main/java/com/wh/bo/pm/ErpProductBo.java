package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.ServiceException;
import com.wh.dao.pm.ErpModuleDao;
import com.wh.dao.pm.ErpProductDao;
import com.wh.entity.pm.ErpModule;
import com.wh.entity.pm.ErpProduct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErpProductBo {

    private final ErpProductDao productDao;
    private final ErpModuleDao moduleDao;

    public ErpProductBo(ErpProductDao productDao, ErpModuleDao moduleDao) {
        this.productDao = productDao;
        this.moduleDao = moduleDao;
    }

    public IPage<ErpProduct> pageList(int pageNum, int pageSize, String keyword) {
        Page<ErpProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ErpProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProduct::getDelFlag, "0");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ErpProduct::getProductCode, keyword)
                   .or()
                   .like(ErpProduct::getProductName, keyword);
        }
        wrapper.orderByDesc(ErpProduct::getCreateDate);
        IPage<ErpProduct> result = productDao.selectPage(page, wrapper);
        fillModuleCounts(result.getRecords());
        return result;
    }

    public List<ErpProduct> listAll() {
        LambdaQueryWrapper<ErpProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProduct::getDelFlag, "0");
        wrapper.orderByDesc(ErpProduct::getCreateDate);
        List<ErpProduct> list = productDao.selectList(wrapper);
        fillModuleCounts(list);
        return list;
    }

    private void fillModuleCounts(List<ErpProduct> products) {
        for (ErpProduct p : products) {
            LambdaQueryWrapper<ErpModule> w = new LambdaQueryWrapper<>();
            w.eq(ErpModule::getProductId, p.getId());
            w.eq(ErpModule::getDelFlag, "0");
            p.setModuleCount(moduleDao.selectCount(w).intValue());
        }
    }

    public ErpProduct getById(String id) {
        ErpProduct product = productDao.selectById(id);
        if (product == null || "1".equals(product.getDelFlag())) {
            throw new ServiceException(404, "产品不存在");
        }
        return product;
    }

    public ErpProduct getByProductCode(String productCode) {
        LambdaQueryWrapper<ErpProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProduct::getProductCode, productCode);
        wrapper.eq(ErpProduct::getDelFlag, "0");
        return productDao.selectOne(wrapper);
    }

    @Transactional
    public ErpProduct create(ErpProduct product) {
        // Check uniqueness
        ErpProduct existing = getByProductCode(product.getProductCode());
        if (existing != null) {
            throw new ServiceException(409, "产品编码已存在");
        }
        if (product.getStatus() == null || product.getStatus().isEmpty()) {
            product.setStatus("ACTIVE");
        }
        product.setDelFlag("0");
        product.setVerNo(0);
        productDao.insert(product);
        return product;
    }

    @Transactional
    public void update(String id, ErpProduct req) {
        ErpProduct product = getById(id);
        product.setProductName(req.getProductName());
        product.setProductVersion(req.getProductVersion());
        product.setVersionReleaseDate(req.getVersionReleaseDate());
        product.setStatus(req.getStatus());
        product.setDescription(req.getDescription());
        product.setRemarks(req.getRemarks());
        productDao.updateById(product);
    }

    @Transactional
    public void delete(String id) {
        getById(id);
        productDao.deleteById(id);
    }

    /**
     * Create a placeholder product for import when product code doesn't exist.
     */
    @Transactional
    public ErpProduct createPlaceholder(String productCode) {
        ErpProduct product = new ErpProduct();
        product.setProductCode(productCode);
        product.setProductName(productCode);
        product.setStatus("PLACEHOLDER");
        product.setDelFlag("0");
        product.setVerNo(0);
        productDao.insert(product);
        return product;
    }
}
