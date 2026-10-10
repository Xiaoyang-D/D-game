package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.dto.GameBoardSaveReq;
import com.xiaoyang.d_game.dto.GameSectionSaveReq;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.GameBoardSetting;
import com.xiaoyang.d_game.entity.GameCategory;
import com.xiaoyang.d_game.mapper.BoardMapper;
import com.xiaoyang.d_game.mapper.GameBoardSettingMapper;
import com.xiaoyang.d_game.mapper.GameCategoryMapper;
import com.xiaoyang.d_game.mapper.GameMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 管理游戏版区及独立分区设置，事务保留已有帖子关联。 */
@Service
@RequiredArgsConstructor
public class GameSectionService {
    private final GameMapper games;
    private final BoardMapper boards;
    private final GameBoardSettingMapper settings;
    private final GameCategoryMapper categories;
    private static final List<String> DEFAULT_NAMES = List.of("论坛", "官方", "攻略", "互助", "同人", "COS");
    private static final Map<String, String> DEFAULT_ICONS = Map.of(
            "论坛", "forum", "官方", "official", "攻略", "guide", "互助", "help", "同人", "art", "COS", "camera");

    public PageResult<Game> page(long page, long size) {
        Page<Game> result = games.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Game>()
                .orderByAsc(Game::getSortOrder).orderByAsc(Game::getId));
        PageResult<Game> response = new PageResult<>();
        response.setPage(result.getCurrent()); response.setSize(result.getSize());
        response.setTotal(result.getTotal()); response.setRecords(result.getRecords());
        return response;
    }

    public Game requireGame(Long id) {
        Game game = games.selectById(id);
        if (game == null) { throw new BizException(ResultCode.GAME_NOT_FOUND); }
        return game;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(GameSectionSaveReq req) {
        Game game = new Game();
        applyGame(game, req);
        if (game.getCategoryId() == null) {
            List<GameCategory> defaults = categories.selectList(new LambdaQueryWrapper<GameCategory>()
                    .orderByAsc(GameCategory::getSortOrder).last("LIMIT 1"));
            if (defaults.isEmpty()) { throw new BizException(ResultCode.BAD_REQUEST, "请先建立游戏分类"); }
            game.setCategoryId(defaults.get(0).getId());
        }
        games.insert(game);
        initializeBoards(game.getId());
        return game.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GameSectionSaveReq req) {
        Game game = requireGame(id);
        applyGame(game, req);
        games.updateById(game);
    }

    private void applyGame(Game game, GameSectionSaveReq req) {
        if (req.getCategoryId() != null && categories.selectById(req.getCategoryId()) == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "游戏分类不存在");
        }
        game.setName(req.getName().trim()); game.setEnglishName(req.getEnglishName().trim());
        game.setDescription(req.getDescription()); game.setIconUrl(req.getIconUrl());
        game.setBannerUrl(req.getBannerUrl()); game.setSortOrder(req.getSortOrder()); game.setEnabled(req.getEnabled());
        if (req.getCategoryId() != null) { game.setCategoryId(req.getCategoryId()); }
    }

    @Transactional(rollbackFor = Exception.class)
    public void initializeBoards(Long gameId) {
        for (int index = 0; index < DEFAULT_NAMES.size(); index++) {
            String name = DEFAULT_NAMES.get(index);
            Board board = boards.selectOne(new LambdaQueryWrapper<Board>().eq(Board::getName, name)
                    .eq(Board::getScopePrivate, false).last("LIMIT 1"));
            if (board == null) {
                board = new Board(); board.setName(name); board.setSortOrder((index + 1) * 10);
                board.setPublishPolicy("官方".equals(name) ? "ADMIN" : "LOGIN"); boards.insert(board);
            }
            GameBoardSetting setting = new GameBoardSetting();
            setting.setGameId(gameId); setting.setBoardId(board.getId()); setting.setName(name);
            setting.setDescription(board.getDescription()); setting.setSortOrder((index + 1) * 10);
            setting.setIconKey(DEFAULT_ICONS.get(name)); setting.setPublishPolicy(board.getPublishPolicy());
            settings.insert(setting);
        }
    }

    public List<GameBoardSetting> listSettings(Long gameId, boolean includeDisabled) {
        requireGame(gameId);
        LambdaQueryWrapper<GameBoardSetting> query = new LambdaQueryWrapper<GameBoardSetting>()
                .eq(GameBoardSetting::getGameId, gameId);
        if (!includeDisabled) { query.eq(GameBoardSetting::getEnabled, true); }
        return settings.selectList(query.orderByAsc(GameBoardSetting::getSortOrder).orderByAsc(GameBoardSetting::getId));
    }

    public List<Board> publicBoards(Long gameId) {
        if (!Boolean.TRUE.equals(requireGame(gameId).getEnabled())) { return List.of(); }
        return listSettings(gameId, false).stream().map(setting -> {
            Board board = new Board();
            board.setId(setting.getBoardId()); board.setName(setting.getName()); board.setDescription(setting.getDescription());
            board.setIconKey(setting.getIconKey()); board.setIconUrl(setting.getIconUrl()); board.setBannerUrl(setting.getBannerUrl());
            board.setSortOrder(setting.getSortOrder()); board.setEnabled(setting.getEnabled());
            board.setPublishPolicy(setting.getPublishPolicy());
            return board;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createBoard(Long gameId, GameBoardSaveReq req) {
        requireGame(gameId);
        // 全局 board 表名称唯一；私有分区使用内部名称，展示名称仅保存在游戏设置中。
        Board board = new Board(); board.setName("section-" + java.util.UUID.randomUUID()); board.setScopePrivate(true);
        board.setPublishPolicy(req.getPublishPolicy()); boards.insert(board);
        GameBoardSetting setting = new GameBoardSetting();
        BeanUtils.copyProperties(req, setting); setting.setName(req.getName().trim());
        setting.setGameId(gameId); setting.setBoardId(board.getId()); settings.insert(setting);
        return board.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBoard(Long gameId, Long boardId, GameBoardSaveReq req) {
        requireGame(gameId);
        GameBoardSetting setting = findSetting(gameId, boardId);
        if (setting == null) { throw new BizException(ResultCode.BOARD_NOT_FOUND); }
        BeanUtils.copyProperties(req, setting); setting.setName(req.getName().trim());
        settings.updateById(setting);
    }

    public GameBoardSetting findSetting(Long gameId, Long boardId) {
        return settings.selectOne(new LambdaQueryWrapper<GameBoardSetting>()
                .eq(GameBoardSetting::getGameId, gameId).eq(GameBoardSetting::getBoardId, boardId));
    }

    public boolean isAdministrator(Long userId) {
        return settings.countAdministrator(userId) > 0;
    }

    public void validatePublishing(Long gameId, Board board, boolean administrator) {
        String policy = board.getPublishPolicy();
        if (gameId == null) {
            if (Boolean.TRUE.equals(board.getScopePrivate())) {
                throw new BizException(ResultCode.BAD_REQUEST, "请选择该分区所属游戏");
            }
        } else {
            Game game = games.selectByIdForUpdate(gameId);
            if (game == null) { throw new BizException(ResultCode.GAME_NOT_FOUND); }
            if (!Boolean.TRUE.equals(game.getEnabled())) { throw new BizException(ResultCode.FORBIDDEN, "游戏版区已停用"); }
            GameBoardSetting setting = settings.selectOne(new LambdaQueryWrapper<GameBoardSetting>()
                    .eq(GameBoardSetting::getGameId, gameId).eq(GameBoardSetting::getBoardId, board.getId()).last("FOR UPDATE"));
            if (setting == null) { throw new BizException(ResultCode.BAD_REQUEST, "分区不属于当前游戏"); }
            if (!Boolean.TRUE.equals(setting.getEnabled())) { throw new BizException(ResultCode.FORBIDDEN, "分区已停用"); }
            policy = setting.getPublishPolicy();
        }
        if (Objects.equals(policy, "ADMIN") && !administrator) {
            throw new BizException(ResultCode.FORBIDDEN, "该分区仅管理员可以发布");
        }
    }
}
