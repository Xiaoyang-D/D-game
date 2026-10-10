package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.mapper.BoardMapper;
import com.xiaoyang.d_game.service.BoardService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 社区版块业务实现。
 *
 * <p>版块属于基础字典数据，当前只提供按排序字段读取的能力。</p>
 */
public class BoardServiceImpl extends ServiceImpl<BoardMapper, Board> implements BoardService {

    @Override
    /**
     * 查询版块列表。
     */
    public List<Board> listBoards() {
        return list(new LambdaQueryWrapper<Board>().eq(Board::getScopePrivate, false).orderByAsc(Board::getSortOrder));
    }
}
