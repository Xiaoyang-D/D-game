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

/**
 * 游戏库业务接口。
 *
 * <p>承载游戏浏览、排行、详情、创建、评分和基础字典查询。
 * 实现类会在游戏响应中聚合分类和标签信息，评分写入后会重新计算平均分和评分人数。</p>
 */
public interface GameService extends IService<Game> {

    /**
     * 按分类、标签和关键字分页查询游戏。
     */
    PageResult<GameResp> pageGames(GameQueryReq req);

    /**
     * 查询游戏排行榜。
     */
    PageResult<GameResp> pageRanking(GameRankingQueryReq req);

    /**
     * 获取游戏详情。
     */
    GameResp getGameDetail(Long gameId);

    /**
     * 创建游戏并保存标签关联。
     */
    Long createGame(GameCreateReq req);

    /**
     * 当前用户对游戏评分或更新已有评分。
     */
    void rateGame(Long gameId, GameRatingReq req);

    /**
     * 分页查询某个游戏下的评测。
     */
    PageResult<GameReviewResp> pageGameReviews(Long gameId, Long page, Long size);

    /**
     * 查询游戏分类字典。
     */
    List<GameCategory> listCategories();

    /**
     * 查询游戏标签字典。
     */
    List<Tag> listTags();
}
