package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.entity.Board;

import java.util.List;

public interface BoardService extends IService<Board> {

    List<Board> listBoards();
}
