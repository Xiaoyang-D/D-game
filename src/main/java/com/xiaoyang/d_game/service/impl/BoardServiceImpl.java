package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.mapper.BoardMapper;
import com.xiaoyang.d_game.service.BoardService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardServiceImpl extends ServiceImpl<BoardMapper, Board> implements BoardService {

    @Override
    public List<Board> listBoards() {
        return list(new LambdaQueryWrapper<Board>().orderByAsc(Board::getSortOrder));
    }
}
