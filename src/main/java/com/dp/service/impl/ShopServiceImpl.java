package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dp.dto.Result;
import com.dp.entity.Shop;
import com.dp.mapper.ShopMapper;
import com.dp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.utils.CacheClient;
import com.dp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
        // Shop shop = cacheClient
        //        .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
         Shop shop = cacheClient
                 .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);


        if(shop==null){
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    public void save2Redis(Long id,Long expireSeconds){
        Shop shop = getById(id);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));
    }
    @Override
    @Transactional
    public Result update(Shop shop) {
        if(shop.getId()==null){
            return Result.fail("店铺 id 不能为空");
        }
        //旁路缓存
        //先删除数据库
        //再删除缓存
        updateById(shop);
        System.out.println("更新成功");
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        System.out.println("缓存删除成功");
        return Result.ok();
    }
}



//解决缓存穿透
//Shop shop = queryWithPathThrough(id);
//互斥锁解决缓存击穿
//Shop shop = queryWithMutex(id);
//逻辑锁解决缓存击穿
//Shop shop = queryWithLogicalExpire(id);
//public Shop queryWithLogicalExpire(Long id) {
//    String key = CACHE_SHOP_KEY + id;
//    //1. redis
//    String cacheShop = stringRedisTemplate.opsForValue().get(key);
//    //2.判断存在
//    if(StrUtil.isBlank(cacheShop)){
//        //3.不存在返回null
//        return null;
//    }
//    //4.命中，JSON反序列化对象，判断是否过期
//    RedisData redisData = JSONUtil.toBean(cacheShop, RedisData.class);
//    Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//    LocalDateTime expireTime = redisData.getExpireTime();
//    if(expireTime.isAfter(LocalDateTime.now())){
//        //没过期
//        return shop;
//    }
//
//    //过期
//    String lockKey = LOCK_SHOP_KEY + id;
//    boolean flag = tryLock(lockKey);
//    //重新生成
//    if(flag){
//        CACHE_REBUILD_EXECUTOR.submit(()->{
//            try{
//                this.save2Redis(id,30L);
//            }
//            catch (Exception e){
//                throw new RuntimeException(e);
//            }
//            finally {
//                unLock(lockKey);
//            }
//        });
//    }
//
//    return shop;
//}
//public Shop queryWithMutex(Long id) {
//    String key = CACHE_SHOP_KEY + id;
//    String lockKey = LOCK_SHOP_KEY + id;
//    Shop shop = null;
//    try {
//        while(true){
//            //1. redis
//            String cacheShop = stringRedisTemplate.opsForValue().get(key);
//            //2.判断存在
//            if(StrUtil.isNotBlank(cacheShop)){
//                //3.存在返回
//                return JSONUtil.toBean(cacheShop,Shop.class);
//            }
//            //当时空字符串时，直接返回错误，而不访问数据库
//            if("".equals(cacheShop)){
//                return null;
//            }
//
//
//            boolean flag = tryLock(lockKey);
//
//            if(flag){
//                break;
//            }
//            else{
//                Thread.sleep(50);
//            }
//        }
//        //4.不返回，查询数据库
//        shop = getById(id);
//        Thread.sleep(200);
//        //4.1.不存在，返回错误
//        if(shop==null){
//            //为防止缓存穿透，可以在数据库汇总设置空值
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//            return null;
//        }
//        //4.2.存在，返回，并写入redis
//        stringRedisTemplate.opsForValue().set(
//                CACHE_SHOP_KEY + shop.getId(),
//                JSONUtil.toJsonStr(shop),
//                CACHE_SHOP_TTL,
//                TimeUnit.MINUTES
//        );
//    }
//    catch (InterruptedException e){
//        throw new RuntimeException(e);
//    }finally {
//        unLock(lockKey);
//    }
//
//    //5.返回
//    return shop;
//}
//public Shop queryWithPathThrough(Long id) {
//    String key = CACHE_SHOP_KEY + id;
//    //1. redis
//    String cacheShop = stringRedisTemplate.opsForValue().get(key);
//    //2.判断存在
//    if(StrUtil.isNotBlank(cacheShop)){
//        //3.存在返回
//        return JSONUtil.toBean(cacheShop,Shop.class);
//    }
//    //当时空字符串时，直接返回错误，而不访问数据库
//    if("".equals(cacheShop)){
//        return null;
//    }
//    //4.不返回，查询数据库
//    Shop shop = getById(id);
//    //4.1.不存在，返回错误
//    if(shop==null){
//        //为防止缓存穿透，可以在数据库汇总设置空值
//        stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//        return null;
//    }
//    //4.2.存在，返回，并写入redis
//    stringRedisTemplate.opsForValue().set(
//            CACHE_SHOP_KEY + shop.getId(),
//            JSONUtil.toJsonStr(shop),
//            CACHE_SHOP_TTL,
//            TimeUnit.MINUTES
//    );
//    //5.返回
//    return shop;
//}
//private boolean tryLock(String key){
//    Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//    return BooleanUtil.isTrue(flag);
//}
//
//private void unLock(String key){
//    stringRedisTemplate.delete(key);
//}
