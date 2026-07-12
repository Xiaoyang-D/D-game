package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.GameCreateReq;
import com.xiaoyang.d_game.dto.GameQueryReq;
import com.xiaoyang.d_game.dto.GameRankingQueryReq;
import com.xiaoyang.d_game.dto.GameRatingReq;
import com.xiaoyang.d_game.dto.GameReviewResp;
import com.xiaoyang.d_game.dto.GameResp;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.GameCategory;
import com.xiaoyang.d_game.entity.Tag;

import java.util.List;

public interface GameService extends IService<Game> {

    PageResult<GameResp> pageGames(GameQueryReq req);

    PageResult<GameResp> pageRanking(GameRankingQueryReq req);

    GameResp getGameDetail(Long gameId);

    Long createGame(GameCreateReq req);

    void rateGame(Long gameId, GameRatingReq req);

    PageResult<GameReviewResp> pageGameReviews(Long gameId, Long page, Long size);

    List<GameCategory> listCategories();

    List<Tag> listTags();
}
