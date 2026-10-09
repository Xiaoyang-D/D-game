package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.common.enums.SearchTypeEnum;
import com.xiaoyang.d_game.dto.GameQueryReq;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.SearchQueryReq;
import com.xiaoyang.d_game.dto.SearchResp;
import com.xiaoyang.d_game.service.GameService;
import com.xiaoyang.d_game.service.PostService;
import com.xiaoyang.d_game.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * 统一搜索实现。
 *
 * <p>该服务不直接访问 Mapper，而是复用游戏和帖子已有分页能力，确保搜索结果和普通列表遵循同样的可见性规则。</p>
 */
public class SearchServiceImpl implements SearchService {

    private final GameService gameService;
    private final PostService postService;

    @Override
    /**
     * 按搜索类型查询游戏、帖子或全部。
     */
    public SearchResp search(SearchQueryReq req) {
        SearchTypeEnum type = SearchTypeEnum.fromCode(req.getType());
        SearchResp resp = new SearchResp();

        if (req.getGameId() == null && (type == SearchTypeEnum.GAME || type == SearchTypeEnum.ALL)) {
            // 复用游戏列表查询，保持分类补全、标签补全和分页结构一致。
            GameQueryReq gameReq = new GameQueryReq();
            gameReq.setKeyword(req.getKeyword());
            gameReq.setPage(req.getPage());
            gameReq.setSize(req.getSize());
            resp.setGames(gameService.pageGames(gameReq));
        }

        if (type == SearchTypeEnum.POST || type == SearchTypeEnum.ALL) {
            // 复用公开帖子列表查询，确保未审核帖子不会被搜索出来。
            PostQueryReq postReq = new PostQueryReq();
            postReq.setGameId(req.getGameId());
            postReq.setBoardId(req.getBoardId());
            postReq.setRecommended(req.getRecommended());
            postReq.setKeyword(req.getKeyword());
            postReq.setPage(req.getPage());
            postReq.setSize(req.getSize());
            resp.setPosts(postService.pagePosts(postReq));
        }

        return resp;
    }
}
