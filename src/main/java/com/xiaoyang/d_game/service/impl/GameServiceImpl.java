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
import com.xiaoyang.d_game.security.UserContext;
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
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

    private final GameCategoryMapper gameCategoryMapper;
    private final TagMapper tagMapper;
    private final GameTagRelMapper gameTagRelMapper;
    private final GameRatingMapper gameRatingMapper;
    private final UserMapper userMapper;

    @Override
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
    public GameResp getGameDetail(Long gameId) {
        Game game = getBaseMapper().selectByIdForUpdate(gameId);
        if (game == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        return toGameResp(game);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    public void rateGame(Long gameId, GameRatingReq req) {
        Long userId = UserContext.getUserId();
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
            GameRating rating = new GameRating();
            rating.setUserId(userId);
            rating.setGameId(gameId);
            rating.setScore(req.getScore());
            applyReviewFields(rating, req);
            gameRatingMapper.insert(rating);
        } else {
            existing.setScore(req.getScore());
            applyReviewFields(existing, req);
            gameRatingMapper.updateById(existing);
        }
        refreshGameRating(game);
    }

    @Override
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
    public List<GameCategory> listCategories() {
        return gameCategoryMapper.selectList(new LambdaQueryWrapper<GameCategory>()
                .orderByAsc(GameCategory::getSortOrder));
    }

    @Override
    public List<Tag> listTags() {
        return tagMapper.selectList(null);
    }

    private void refreshGameRating(Game game) {
        Map<String, Object> summary = gameRatingMapper.selectRatingSummary(game.getId());
        Number average = (Number) summary.get("avgRating");
        Number count = (Number) summary.get("ratingCount");
        game.setAvgRating(BigDecimal.valueOf(average == null ? 0 : average.doubleValue())
                .setScale(2, RoundingMode.HALF_UP));
        game.setRatingCount(count == null ? 0 : count.intValue());
        updateById(game);
    }

    private void applyReviewFields(GameRating rating, GameRatingReq req) {
        rating.setSummary(normalizeText(req.getSummary()));
        rating.setPros(normalizeText(req.getPros()));
        rating.setCons(normalizeText(req.getCons()));
        rating.setPlaytimeHours(req.getPlaytimeHours());
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

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

    private GameResp toGameResp(Game game) {
        return toGameResps(List.of(game)).getFirst();
    }

    private List<GameResp> toGameResps(List<Game> games) {
        if (games.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> gameIds = games.stream().map(Game::getId).toList();
        Map<Long, String> categoryNames = gameCategoryMapper.selectBatchIds(
                        games.stream().map(Game::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(GameCategory::getId, GameCategory::getName));
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
