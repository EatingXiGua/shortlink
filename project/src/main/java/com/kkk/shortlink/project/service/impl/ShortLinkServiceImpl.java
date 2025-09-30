package com.kkk.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkk.shortlink.project.common.convention.exception.ServiceException;
import com.kkk.shortlink.project.dao.entity.ShortLinkDO;
import com.kkk.shortlink.project.dao.mapper.ShortLinkMapper;
import com.kkk.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.kkk.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.kkk.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.kkk.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.kkk.shortlink.project.service.ShortLinkService;
import com.kkk.shortlink.project.toolkit.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 短链接接口实现层
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;


    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);//短链接
        //String fullShortUrl = requestParam.getDomain()+"/"+shortLinkSuffix;//完整的短链接（域名+短链接）
        String fullShortUrl = StrBuilder.create(requestParam.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .build();
//        ShortLinkDO shortLinkDO = BeanUtil.toBean(requestParam, ShortLinkDO.class);
//        shortLinkDO.setFullShortUrl(fullShortUrl);
//        shortLinkDO.setEnableStatus(0);
//        shortLinkDO.setShortUri(shortLinkSuffix);

        // 一点思考：generateSuffix 方法中生成的是唯一的短链接，为什么这里还需要再一次查数据库保证唯一？
        // generateSuffix 方法生成的短链接能够保证一定是不存在于缓存中
        // 有种可能，短链接入库成功，但是却没有添加到布隆过滤器中（进程挂掉等等原因），也就是说，短链接实际上入库了，但是布隆过滤器显示不存在
        // 此时插入这个短链接就越过了布隆过滤器，被数据库的唯一索引拦截
        try {
            baseMapper.insert(shortLinkDO);//往db存
        } catch (DuplicateKeyException e) {
            //兜底处理数据库唯一约束冲突，防止短链接重复生成。即使布隆过滤器和本地生成逻辑没拦住，也能依赖数据库的唯一约束兜底。
            //布隆过滤器存在误判 不存在的一定不存在，存在的不一定存在
            // 误判的怎么处理？
            // 第一种，要添加的短链接确实真实存在缓存 那确实就是重复入库
            // 第二种，要添加的短链接不一定存在缓存 那就不是重复入库了
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null) {
                log.warn("短链接：{} 重复入库", fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);//将完整的短链接（域名+短链接）添加到布隆过滤器
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        int customGenerateCount = 0;
        String shortUri;
        while (true){
            if (customGenerateCount > 10){
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            //判断生成的短链接是否存在于布隆过滤器 完整的短链接不能重复 如果存在说明重复
            if (!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain()+"/"+ shortUri)){
                break;//如果布隆过滤器中不包括新生成的这个短链接，那这个就可用，跳出while循环
            }
            customGenerateCount++;
        }
        return shortUri;
    }
}
