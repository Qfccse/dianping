package com.dp.service.impl;

import com.dp.dto.Result;
import com.dp.entity.SeckillVoucher;
import com.dp.entity.VoucherOrder;
import com.dp.mapper.VoucherOrderMapper;
import com.dp.service.ISeckillVoucherService;
import com.dp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.utils.RedisIdWorker;
import com.dp.utils.SimpleRedisLock;
import com.dp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
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
    private VoucherOrderServiceImpl service;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断是否开始、是否结束、是否已抢完
        if(LocalDateTime.now().isBefore(voucher.getBeginTime())){
            return Result.fail("秒杀未开始");
        }
        if(LocalDateTime.now().isAfter(voucher.getEndTime())){
            return Result.fail("秒杀已结束");
        }
        if(voucher.getStock()<=0){
            return Result.fail("优惠券已抢完");
        }
        Long uid = UserHolder.getUser().getId();
        //SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + uid, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order");
        boolean isLock = lock.tryLock();
        if(!isLock){
            return Result.fail("不允许重复抢单");
        }

        try {
            synchronized (uid.toString().intern()) {
                //IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
                return service.createVoucherOrder(voucherId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        //判断是否已经抢过
        Long uid = UserHolder.getUser().getId();
        int count = query().eq("user_id",uid).eq("voucher_id",voucherId).count();
        if(count>0){
            return Result.fail("用户已购买过一次");
        }

        //减少一个
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id",voucherId)
                //.eq("stock",voucher.getStock())
                .gt("stock",0)
                .update();
        //没抢到
        if(!success){
            return Result.fail("优惠券已抢完");
        }


        //抢到了，创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //设置订单参数
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        //写入数据库
        save(voucherOrder);

        return Result.ok(orderId);
    }
}
