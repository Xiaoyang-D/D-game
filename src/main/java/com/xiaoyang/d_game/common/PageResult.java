package com.xiaoyang.d_game.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
/**
 * 统一分页响应结构。
 *
 * <p>用于承接 MyBatis-Plus 的分页查询结果，并把分页信息转换成前端稳定字段：
 * 当前页、每页条数、总记录数和当前页数据列表。</p>
 *
 * @param <T> 当前页中的记录类型，通常是响应 DTO
 */
public class PageResult<T> implements Serializable {

    /** 当前页码，和 MyBatis-Plus 的 current 保持一致。 */
    private long page;

    /** 每页条数。 */
    private long size;

    /** 符合查询条件的总记录数。 */
    private long total;

    /** 当前页数据；空结果时统一返回空列表，避免前端判空成本。 */
    private List<T> records;

    /**
     * 将 MyBatis-Plus 分页对象转换成统一响应结构。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 构造一个空分页结果。
     *
     * <p>当业务前置条件已经确定没有结果时，可以直接返回空分页，避免执行无意义的数据库分页查询。</p>
     */
    public static <T> PageResult<T> empty(long page, long size) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(0);
        result.setRecords(Collections.emptyList());
        return result;
    }
}
