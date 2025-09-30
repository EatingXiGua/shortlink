package com.kkk.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kkk.shortlink.project.dao.entity.ShortLinkDO;
import com.kkk.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.kkk.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.kkk.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.kkk.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 短链接接口层
 */
public interface ShortLinkService extends IService<ShortLinkDO> {

    /**
     * 创建短链接
     * @param requestParam 创建短链接请求信息
     * return 创建短链接信息
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 短链接分页查询
     * @param requestParam 短链接分页查询请求参数
     * @return 短链接分页查询返回信息
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
}
