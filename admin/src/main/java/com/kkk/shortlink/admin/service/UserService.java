package com.kkk.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kkk.shortlink.admin.dao.entity.UserDO;
import com.kkk.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.kkk.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户返回实体
     */
    UserRespDTO getUserByUserName(String username);

    /**
     * 查询用户名是否存在
     * @param username 用户名
     * @return 存在返回true 不存在返回false
     */
    Boolean hasUsername(String username);

    /**
     * 注册用户请求参数
     * @param requestParam
     */
    void register(UserRegisterReqDTO requestParam);
}
