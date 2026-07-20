package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.GameRankingTypeEnum;
import com.xiaoyang.d_game.dto.GameCreateReq;
import com.xiaoyang.d_game.dto.GameQueryReq;
import com.xiaoyang.d_game.dto.GameRankingQueryReq;
import com.xiaoyang.d_game.dto.GameRatingReq;
import com.xiaoyang.d_game.dto.GameReviewResp;
import com.xiaoyang.d_game.dto.GameResp;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.GameCategory;
import com.xiaoyang.d_game.entity.GameRating;
import com.xiaoyang.d_game.entity.GameTagRel;
import com.xiaoyang.d_game.entity.Tag;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.mapper.GameCategoryMapper;
import com.xiaoyang.d_game.mapper.GameMapper;
import com.xiaoyang.d_game.mapper.GameRatingMapper;
import com.xiaoyang.d_game.mapper.GameTagRelMapper;
import com.xiaoyang.d_game.mapper.TagMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 游戏库业务实现。
 *
 * <p>负责游戏查询、排行榜、详情、创建和评分。为了减少前端请求次数，列表和详情响应会批量补充分类名称和标签名称；
 * 用户评分后会立即回算游戏平均分和评分人数。</p>
 */
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

    private final GameCategoryMapper gameCategoryMapper;
    private final TagMapper tagMapper;
    private final GameTagRelMapper gameTagRelMapper;
    private final GameRatingMapper gameRatingMapper;
    private final UserMapper userMapper;

    @Override
    /**
     * 分页查询游戏列表。
     *
     * <p>支持分类、关键字和标签筛选。标签筛选使用 EXISTS 子查询，避免把关联表 join 后影响分页记录数。</p>
     */
    public PageResult<GameResp> pageGames(GameQueryReq req) {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        if (req.getCategoryId() != null) {
            wrapper.eq(Game::getCategoryId, req.getCategoryId());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.like(Game::getName, req.getKeyword());
        }
        if (req.getTagId() != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM game_tag_rel gtr WHERE gtr.game_id = id "
                    + "AND gtr.tag_id = {0} AND gtr.is_deleted = 0)", req.getTagId());
        }
        wrapper.orderByDesc(Game::getId);
        Page<Game> page = page(new Page<>(req.getPage(), req.getSize()), wrapper);
        List<GameResp> records = toGameResps(page.getRecords());
        PageResult<GameResp> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    /**
     * 游戏排行榜。
     *
     * <p>popular 偏向评分人数，rating 偏向平均分；两个排序都会使用另一个指标做次级排序，让结果更稳定。</p>
     */
    public PageResult<GameResp> pageRanking(GameRankingQueryReq req) {
        GameRankingTypeEnum rankingType = GameRankingTypeEnum.fromCode(req.getType());
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        if (rankingType == GameRankingTypeEnum.POPULAR) {
            wrapper.orderByDesc(Game::getRatingCount)
                    .orderByDesc(Game::getAvgRating);
        } else {
            wrapper.orderByDesc(Game::getAvgRating)
                    .orderByDesc(Game::getRatingCount);
        }
        Page<Game> page = page(new Page<>(req.getPage(), req.getSize()), wrapper);
        List<GameResp> records = toGameResps(page.getRecords());
        PageResult<GameResp> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    /**
     * 查询游戏详情。
     */
    public GameResp getGameDetail(Long gameId) {
        Game game = getBaseMapper().selectByIdForUpdate(gameId);
        if (game == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        return toGameResp(game);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建游戏。
     *
     * <p>事务覆盖游戏主表和游戏-标签关系表，保证标签关联不会出现只插入一半的情况。</p>
     */
    public Long createGame(GameCreateReq req) {
        Game game = new Game();
        game.setName(req.getName());
        game.setCategoryId(req.getCategoryId());
        game.setCoverUrl(req.getCoverUrl());
        game.setDescription(req.getDescription() == null ? "" : req.getDescription());
        game.setDeveloper(req.getDeveloper() == null ? "" : req.getDeveloper());
        game.setReleaseDate(req.getReleaseDate());
        save(game);
        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            // 当前接口信任前端传入的 tagId 已存在；如果后续开放给非管理员，应增加标签存在性校验。
            for (Long tagId : req.getTagIds()) {
                GameTagRel rel = new GameTagRel();
                rel.setGameId(game.getId());
                rel.setTagId(tagId);
                gameTagRelMapper.insert(rel);
            }
        }
        return game.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 新增或更新当前用户对游戏的评分。
     *
     * <p>同一用户对同一游戏只能有一条评分记录；重复评分会覆盖原评分，并在事务内回算游戏汇总评分。</p>
     */
    public void rateGame(Long gameId, GameRatingReq req) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Game game = getById(gameId);
        if (game == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        GameRating existing = gameRatingMapper.selectOne(new LambdaQueryWrapper<GameRating>()
                .eq(GameRating::getUserId, userId)
                .eq(GameRating::getGameId, gameId)
                .last("LIMIT 1"));
        if (existing == null) {
            // 首次评分：插入一条用户-游戏评分记录。
            GameRating rating = new GameRating();
            rating.setUserId(userId);
            rating.setGameId(gameId);
            rating.setScore(req.getScore());
            applyReviewFields(rating, req);
            gameRatingMapper.insert(rating);
        } else {
            // 再次评分：更新原记录，避免一个用户重复拉高评分人数。
            existing.setScore(req.getScore());
            applyReviewFields(existing, req);
            gameRatingMapper.updateById(existing);
        }
        refreshGameRating(game);
    }

    @Override
    /**
     * 分页查询游戏评测。
     */
    public PageResult<GameReviewResp> pageGameReviews(Long gameId, Long page, Long size) {
        if (getById(gameId) == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        Page<GameRating> result = gameRatingMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<GameRating>()
                        .eq(GameRating::getGameId, gameId)
                        .orderByDesc(GameRating::getGmtModified));
        PageResult<GameReviewResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream().map(this::toReviewResp).toList());
        return pageResult;
    }

    @Override
    /**
     * 查询游戏分类，按排序字段升序。
     */
    public List<GameCategory> listCategories() {
        return gameCategoryMapper.selectList(new LambdaQueryWrapper<GameCategory>()
                .orderByAsc(GameCategory::getSortOrder));
    }

    @Override
    /**
     * 查询全部标签。
     */
    public List<Tag> listTags() {
        return tagMapper.selectList(null);
    }

    /**
     * 回算游戏平均分和评分人数。
     *
     * <p>评分详情表是事实来源，游戏表中的 avgRating/ratingCount 是冗余汇总字段，方便列表排序和展示。</p>
     */
    private void refreshGameRating(Game game) {
        Map<String, Object> summary = gameRatingMapper.selectRatingSummary(game.getId());
        Number average = (Number) summary.get("avgRating");
        Number count = (Number) summary.get("ratingCount");
        game.setAvgRating(BigDecimal.valueOf(average == null ? 0 : average.doubleValue())
                .setScale(2, RoundingMode.HALF_UP));
        game.setRatingCount(count == null ? 0 : count.intValue());
        updateById(game);
    }

    /**
     * 复制评分附加字段，并统一做空字符串处理。
     */
    private void applyReviewFields(GameRating rating, GameRatingReq req) {
        rating.setSummary(normalizeText(req.getSummary()));
        rating.setPros(normalizeText(req.getPros()));
        rating.setCons(normalizeText(req.getCons()));
        rating.setPlaytimeHours(req.getPlaytimeHours());
    }

    /**
     * 将可选文本标准化为非 null 字符串。
     */
    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 将评分实体转换成评测响应，并补充用户昵称。
     */
    private GameReviewResp toReviewResp(GameRating rating) {
        GameReviewResp resp = new GameReviewResp();
        resp.setId(rating.getId());
        resp.setUserId(rating.getUserId());
        resp.setScore(rating.getScore());
        resp.setSummary(rating.getSummary());
        resp.setPros(rating.getPros());
        resp.setCons(rating.getCons());
        resp.setPlaytimeHours(rating.getPlaytimeHours());
        resp.setGmtCreate(rating.getGmtCreate());
        User user = userMapper.selectById(rating.getUserId());
        resp.setUserNickname(user == null ? "已注销用户" : user.getNickname());
        return resp;
    }

    /**
     * 单个游戏实体转换响应对象。
     */
    private GameResp toGameResp(Game game) {
        return toGameResps(List.of(game)).getFirst();
    }

    /**
     * 批量转换游戏响应。
     *
     * <p>这里批量查询分类和标签，避免列表中每个游戏单独查一次导致 N+1 查询。</p>
     */
    private List<GameResp> toGameResps(List<Game> games) {
        if (games.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> gameIds = games.stream().map(Game::getId).toList();
        // 批量加载分类名称，按 categoryId 做内存映射。
        Map<Long, String> categoryNames = gameCategoryMapper.selectBatchIds(
                        games.stream().map(Game::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(GameCategory::getId, GameCategory::getName));
        // 批量加载游戏-标签关系，再批量查询标签名称。
        Map<Long, List<Long>> tagIdsByGame = new HashMap<>();
        List<GameTagRel> relations = gameTagRelMapper.selectList(new LambdaQueryWrapper<GameTagRel>()
                .in(GameTagRel::getGameId, gameIds));
        relations.forEach(rel -> tagIdsByGame.computeIfAbsent(rel.getGameId(), ignored -> new java.util.ArrayList<>())
                .add(rel.getTagId()));
        Map<Long, String> tagNames = tagMapper.selectBatchIds(relations.stream()
                        .map(GameTagRel::getTagId).distinct().toList())
                .stream().collect(Collectors.toMap(Tag::getId, Tag::getName));

        return games.stream().map(game -> toGameResp(game, categoryNames, tagIdsByGame, tagNames)).toList();
    }

    /**
     * 使用已批量加载的分类/标签数据组装单个游戏响应。
     */
    private GameResp toGameResp(Game game, Map<Long, String> categoryNames,
                                Map<Long, List<Long>> tagIdsByGame, Map<Long, String> tagNames) {
        GameResp resp = new GameResp();
        resp.setId(game.getId());
        resp.setName(game.getName());
        resp.setCategoryId(game.getCategoryId());
        resp.setCoverUrl(game.getCoverUrl());
        resp.setDescription(game.getDescription());
        resp.setDeveloper(game.getDeveloper());
        resp.setReleaseDate(game.getReleaseDate());
        resp.setAvgRating(game.getAvgRating());
        resp.setRatingCount(game.getRatingCount());
        resp.setCategoryName(categoryNames.get(game.getCategoryId()));
        resp.setTags(tagIdsByGame.getOrDefault(game.getId(), Collections.emptyList()).stream()
                .map(tagNames::get).filter(java.util.Objects::nonNull).toList());
        return resp;
    }
}
