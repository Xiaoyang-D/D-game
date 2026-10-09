package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.SearchQueryReq;
import com.xiaoyang.d_game.service.GameService;
import com.xiaoyang.d_game.service.PostService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchServiceImplTest {
    @Test
    void selectedGameOnlySearchesPostsInThatGame() {
        GameService games = mock(GameService.class);
        PostService posts = mock(PostService.class);
        SearchQueryReq request = new SearchQueryReq();
        request.setKeyword("攻略");
        request.setType("ALL");
        request.setGameId(42L);
        request.setBoardId(2L);
        request.setRecommended(true);
        request.setPage(2L);
        new SearchServiceImpl(games, posts).search(request);
        var query = ArgumentCaptor.forClass(PostQueryReq.class);
        verify(posts).pagePosts(query.capture());
        assertEquals(42L, query.getValue().getGameId());
        assertEquals(2L, query.getValue().getBoardId());
        assertTrue(query.getValue().getRecommended());
        assertEquals("攻略", query.getValue().getKeyword());
        assertEquals(2L, query.getValue().getPage());
        verifyNoInteractions(games);
    }

    @Test
    void allScopeSearchesGamesAndPostsWithoutGameFilter() {
        GameService games = mock(GameService.class);
        PostService posts = mock(PostService.class);
        SearchQueryReq request = new SearchQueryReq();
        request.setKeyword("明日方舟");
        request.setType("ALL");
        new SearchServiceImpl(games, posts).search(request);
        verify(games).pageGames(any());
        var query = ArgumentCaptor.forClass(PostQueryReq.class);
        verify(posts).pagePosts(query.capture());
        assertNull(query.getValue().getGameId());
    }
}
