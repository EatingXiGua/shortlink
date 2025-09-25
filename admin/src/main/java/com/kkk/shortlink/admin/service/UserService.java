package com.kkk.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kkk.shortlink.admin.dao.entity.UserDO;
import com.kkk.shortlink.admin.dto.req.UserLoginReqDTO;
import com.kkk.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.kkk.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.kkk.shortlink.admin.dto.resp.UserLoginRespDTO;
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
     * 注册用户
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户
     * @param requestParam 修改用户请求参数
     */
    void update(UserUpdateReqDTO requestParam);

    /**
     * 用户登录
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数 token
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * 检查用户是否登录
     * @param username 用户名
     * @param token 用户登录Token
     * @return 登录返回true 未登录返回false
     */
    Boolean checkLogin(String username,String token);
}
