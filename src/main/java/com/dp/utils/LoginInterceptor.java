package com.dp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.LOGIN_USER_TTL;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(UserHolder.getUser()==null){
            //4. 不存在，拦截
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
