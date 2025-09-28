package com.kkk.shortlink.project.dto.req;

import lombok.Data;

import java.util.Date;

/**
 * 创建短链接请求参数
 */
@Data
public class ShortLinkCreateReqDTO {
    /**
     * 域名
     */
    private String domain;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 创建类型 0接口创建 1控制台创建
     */
    private Integer createdType;

    /**
     * 有效期类型 0永久有效 1自定义有效期
     */
    private Integer validDateType;

    /**
     * 有效期
     */
    private Date validDate;

    /**
     * 描述
     */
    private String describe;
}
