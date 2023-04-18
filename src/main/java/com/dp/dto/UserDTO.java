package com.dp.dto;

import com.dp.entity.User;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
