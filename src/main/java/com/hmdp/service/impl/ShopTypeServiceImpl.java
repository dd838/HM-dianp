package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        //1.在Redis中查询是否有商铺类型的缓存
        String key = CACHE_SHOPTYPE_KEY;
        String shopTypeList = stringRedisTemplate.opsForValue().get(key);
        //2.有就直接返回
        if (StrUtil.isNotBlank(shopTypeList)) {
            return JSONUtil.toList(shopTypeList, ShopType.class);
        }
        //3.没有就在数据库查询有没有
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //4.有就写入Redis缓存
        if (typeList != null) {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList), 30, TimeUnit.MINUTES);
        }
        return typeList;
    }
}
