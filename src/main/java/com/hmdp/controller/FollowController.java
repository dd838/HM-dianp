package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.PrivateKey;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PostMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long FollowId, @PathVariable("isFlollow") boolean isFollow) {
        return followService.follow(FollowId, isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFoffow(@PathVariable("id") Long FollowId) {
        return followService.isFoffow(FollowId);
    }
}
