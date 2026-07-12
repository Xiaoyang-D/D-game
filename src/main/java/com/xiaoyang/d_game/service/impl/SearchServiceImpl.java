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
public class SearchServiceImpl implements SearchService {

    private final GameService gameService;
    private final PostService postService;

    @Override
    public SearchResp search(SearchQueryReq req) {
        SearchTypeEnum type = SearchTypeEnum.fromCode(req.getType());
        SearchResp resp = new SearchResp();

        if (type == SearchTypeEnum.GAME || type == SearchTypeEnum.ALL) {
            GameQueryReq gameReq = new GameQueryReq();
            gameReq.setKeyword(req.getKeyword());
            gameReq.setPage(req.getPage());
            gameReq.setSize(req.getSize());
            resp.setGames(gameService.pageGames(gameReq));
        }

        if (type == SearchTypeEnum.POST || type == SearchTypeEnum.ALL) {
            PostQueryReq postReq = new PostQueryReq();
            postReq.setKeyword(req.getKeyword());
            postReq.setPage(req.getPage());
            postReq.setSize(req.getSize());
            resp.setPosts(postService.pagePosts(postReq));
        }

        return resp;
    }
}
