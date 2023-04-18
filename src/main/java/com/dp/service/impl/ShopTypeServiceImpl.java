package com.dp.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dp.dto.Result;
import com.dp.entity.Shop;
import com.dp.entity.ShopType;
import com.dp.mapper.ShopTypeMapper;
import com.dp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dp.utils.RedisConstants.CACHE_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        //1.判断是否存在
        String type = stringRedisTemplate.opsForValue().get(CACHE_TYPE_KEY);
        if(StrUtil.isNotBlank(type)){
            return Result.ok(JSONUtil.toList(type, ShopType.class));
        }
        //2.不存在
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if(typeList==null){
            return Result.fail("分类不存在");
        }
        stringRedisTemplate.opsForValue().set(CACHE_TYPE_KEY,JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }

}
