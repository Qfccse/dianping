package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.User;
import com.dp.mapper.UserMapper;
import com.dp.service.IUserService;
import com.dp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.*;
import static com.dp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        //session.setAttribute(phone,code);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("验证码发送成功，验证码：{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //Object cacheCode = session.getAttribute(loginForm.getPhone());
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        if(cacheCode==null|!loginForm.getCode().equals(cacheCode)){
            return Result.fail("验证码错误");
        }

        User user = userMapper.selectUserByPhone(loginForm.getPhone());
        if(user==null){
            user = createUserWithPhone(loginForm.getPhone());
            System.out.println("不存在用户，创建");
        }

        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        String key = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(key,userMap);
        stringRedisTemplate.expire(key ,LOGIN_USER_TTL,TimeUnit.SECONDS);
        System.out.println(user);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        userMapper.saveNewUser(user);
        return user;
    }
}
