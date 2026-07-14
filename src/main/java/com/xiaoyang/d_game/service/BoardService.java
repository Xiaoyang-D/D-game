package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.entity.Board;

import java.util.List;

/**
 * 社区版块业务接口。
 *
 * <p>继承 MyBatis-Plus {@link IService} 获得基础 CRUD 能力，同时提供面向前端展示的排序列表。</p>
 */
public interface BoardService extends IService<Board> {

    /**
     * 查询所有版块并按 sortOrder 升序排序。
     */
    List<Board> listBoards();
}
