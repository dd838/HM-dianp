package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisWorker redisWorker;

    @Override
    public Result secKillVoucher(Long voucherId) {
        //1. 查询优惠券
        SeckillVoucher byId = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (byId.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动尚未开始");
        }
        //3.判断秒杀是否结束
        if (byId.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动已经结束");
        }
        //4.判断库存是否充足
        if (byId.getStock() < 1) {
            return Result.fail("库存不足");
        }


        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()){
        //7.返回订单id
            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        //一人一单
        Long userId = UserHolder.getUser().getId();
        //查询订单
        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        //判断是否购买过
        if (count > 0) {
            //购买过一次
            return Result.fail("当前用户已经购买过");
        }

        //5.扣减库存
        boolean success = seckillVoucherService
                .update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stocke", 0).update();
        if (!success) {
            //扣减失败
            return Result.fail("库存不足");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //订单id
        Long order = redisWorker.nextId("order");
        voucherOrder.setId(order);
        //用户id
        voucherOrder.setUserId(userId);
        //代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(order);
    }
}