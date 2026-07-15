package com.xiaoyang.d_game.dto;

import com.xiaoyang.d_game.common.PageResult;
import lombok.Data;

import java.util.List;

/** 发布管理页分页数据和四种状态统计。 */
@Data
public class PostManagePageResp {

    /** 已审核通过且未删除的帖子数量。 */
    private long publishedCount;

    /** 等待管理员审核的帖子数量。 */
    private long pendingCount;

    /** 被管理员拒绝的帖子数量。 */
    private long rejectedCount;

    /** 草稿和定时稿数量。 */
    private long draftCount;

    private long page;

    private long size;

    private long total;

    /** 当前状态 Tab 的分页记录。 */
    private List<PostManageItemResp> records;

    public static PostManagePageResp from(PageResult<PostManageItemResp> pageResult) {
        PostManagePageResp response = new PostManagePageResp();
        response.setPage(pageResult.getPage());
        response.setSize(pageResult.getSize());
        response.setTotal(pageResult.getTotal());
        response.setRecords(pageResult.getRecords());
        return response;
    }
}
