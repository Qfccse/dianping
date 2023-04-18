package com.dp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取请求头的token
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            return true;
        }

        String key = RedisConstants.LOGIN_USER_KEY + token;
        //2.获取session中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        //3. 判断用户是否存在
        if (userMap.isEmpty()){
            return true;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //5. 存在 保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);

        stringRedisTemplate.expire(key ,LOGIN_USER_TTL, TimeUnit.SECONDS);
        //6. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
