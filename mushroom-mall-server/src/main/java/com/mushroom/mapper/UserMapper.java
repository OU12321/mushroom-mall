package com.mushroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mushroom.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询用户名总数（包含已逻辑删除的行，绕过 @TableLogic 自动过滤）
     */
    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    Long countAllByUsername(@Param("username") String username);

    /**
     * 物理删除指定用户名的已逻辑删除行（绕过 @TableLogic，直接 DELETE 物理行）
     */
    @Delete("DELETE FROM user WHERE username = #{username} AND is_deleted = 1")
    int physicalDeleteByUsername(@Param("username") String username);
}
