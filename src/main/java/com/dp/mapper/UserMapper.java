package com.dp.mapper;

import com.dp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {
    @Select("select * from tb_user where phone=#{phone}")
    User selectUserByPhone(String phone);

    @Insert("insert into tb_user(id, phone, password, nick_name) values (#{id},#{phone},#{password},#{nickName})")
    void saveNewUser(User user);
}
