package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Override
    public Result follow(Long FollowId, boolean isFlollow) {
        Long userId = UserHolder.getUser().getId();
        //判断是否关注
        if (isFlollow) {
            //关注新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(FollowId);
            save(follow);
        } else {
            //取关删除
            remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", FollowId));
        }
        return Result.ok();
    }

    @Override
    public Result isFoffow(Long FollowId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", FollowId).count();
        return Result.ok(count > 0);
    }
}
