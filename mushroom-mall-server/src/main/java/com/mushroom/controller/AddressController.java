package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Address;
import com.mushroom.service.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 收货地址控制器
 * 用户管理自己的收货地址，下单时可选择已保存地址
 */
@Slf4j
@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 查询当前用户的收货地址列表 (默认地址排最前)
     */
    @GetMapping("/list")
    public Result<List<Address>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId);
        wrapper.orderByDesc(Address::getIsDefault).orderByDesc(Address::getCreateTime);
        return Result.success(addressService.list(wrapper));
    }

    /**
     * 新增收货地址
     * 若设为默认，需要"先把其他地址置为非默认 + 写入新地址"两次写库，故加事务
     */
    @Transactional
    @PostMapping
    public Result<Void> save(@RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        checkAddressFields(address);
        address.setUserId(userId);
        address.setId(null);
        // 若设为默认，先把其他地址置为非默认
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            resetDefault(userId);
        }
        addressService.save(address);
        return Result.success("新增成功", null);
    }

    /**
     * 修改收货地址
     * 同样存在"重置默认 + 更新"两次写库，加事务保证一致性
     */
    @Transactional
    @PutMapping
    public Result<Void> update(@RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (address.getId() == null) {
            throw new BusinessException("地址ID不能为空");
        }
        Address exist = addressService.getById(address.getId());
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        checkAddressFields(address);
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            resetDefault(userId);
        }
        addressService.updateById(address);
        return Result.success("修改成功", null);
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Address exist = addressService.getById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        addressService.removeById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 设为默认地址
     * "清空其他默认 + 设置本地址为默认"两次写库，加事务避免中途失败后用户无默认地址
     */
    @Transactional
    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Address exist = addressService.getById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        resetDefault(userId);
        Address update = new Address();
        update.setId(id);
        update.setIsDefault(1);
        addressService.updateById(update);
        return Result.success("已设为默认地址", null);
    }

    /**
     * 校验必填字段
     */
    private void checkAddressFields(Address address) {
        if (address.getConsignee() == null || address.getConsignee().trim().isEmpty()) {
            throw new BusinessException("收货人不能为空");
        }
        if (address.getPhone() == null || address.getPhone().trim().isEmpty()) {
            throw new BusinessException("联系电话不能为空");
        }
        if (address.getDetailAddress() == null || address.getDetailAddress().trim().isEmpty()) {
            throw new BusinessException("详细地址不能为空");
        }
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
    }

    /**
     * 将该用户的所有地址置为非默认
     */
    private void resetDefault(Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getUserId, userId).set(Address::getIsDefault, 0);
        addressService.update(wrapper);
    }
}
