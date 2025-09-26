package com.kkk.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkk.shortlink.admin.common.convention.exception.ClientException;
import com.kkk.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.kkk.shortlink.admin.dao.entity.UserDO;
import com.kkk.shortlink.admin.dao.mapper.UserMapper;
import com.kkk.shortlink.admin.dto.req.UserLoginReqDTO;
import com.kkk.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.kkk.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.kkk.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.kkk.shortlink.admin.dto.resp.UserRespDTO;
import com.kkk.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.kkk.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.kkk.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.kkk.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserRespDTO getUserByUserName(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO,result);
        return result;
    }

    @Override
    public Boolean hasUsername(String username) {
        //从数据库中查
//        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
//                .eq(UserDO::getUsername,username);
//        UserDO userDO = baseMapper.selectOne(queryWrapper);
//        return null != userDO;
        //通过布隆过滤器判断用户名是否存在
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (hasUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY+requestParam.getUsername());
        try {
            if (lock.tryLock()) {
                int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
                if (inserted < 1) {
                    throw new ClientException(USER_SAVE_ERROR);
                }
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                return;
            }
            throw new ClientException(USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        //TODO 验证当前用户名是否为登录用户
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        //验证用户名和密码是否在数据库中
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag,"0");
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户不存在或者密码错误");
        }
        //判断是否已经登录
        Boolean hasLogin = stringRedisTemplate.hasKey("login_" + requestParam.getUsername());
        if (hasLogin!=null && hasLogin) {
            throw new ClientException("用户已经登录");
        }

        /**
         * Hash
         * Key:login_用户名
         * Value:
         *   Key:uuid 或者说是token
         *   Val:JSON 字符串（用户信息）
         */
        String uuid = UUID.randomUUID().toString();
        //stringRedisTemplate.opsForValue().set(uuid, JSON.toJSONString(userDO),30, TimeUnit.MINUTES);
        //Map<String, Object> userInfoMap = new HashMap<String, Object>();
        //userInfoMap.put("token", JSON.toJSONString(userDO));
        stringRedisTemplate.opsForHash().put("login_"+requestParam.getUsername(), uuid,JSON.toJSONString(userDO));
        stringRedisTemplate.expire("login_"+requestParam.getUsername(),30, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username,String token) {
        //从redis中查询是否存在
//        Object o = stringRedisTemplate.opsForHash().get("login_" + username, token);
//        if (o == null) {
//            System.out.println("1111111111111111111111");
//            return false;
//        }
//        return true;
        return stringRedisTemplate.opsForHash().get("login_" + username, token) != null ;
    }

    @Override
    public void exitLogin(String username,String token) {
        if (checkLogin(username,token)) {
            stringRedisTemplate.delete("login_"+username);
            return;
        }
        throw new ClientException("用户未登录或用户token不存在");
    }
}
