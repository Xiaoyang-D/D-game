package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.dto.GameResp;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.GameCategory;
import com.xiaoyang.d_game.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameServiceImplTest {
    @Test
    void gamesWithoutTagsDoNotGenerateEmptyInQuery() {
        GameCategoryMapper categories = mock(GameCategoryMapper.class);
        TagMapper tags = mock(TagMapper.class);
        GameTagRelMapper relations = mock(GameTagRelMapper.class);
        GameServiceImpl service = new GameServiceImpl(categories, tags, relations,
                mock(GameRatingMapper.class), mock(UserMapper.class));
        Game game = new Game();
        game.setId(2200000000000000001L);
        game.setName("明日方舟");
        game.setCategoryId(3L);
        GameCategory category = new GameCategory();
        category.setId(3L);
        category.setName("策略");
        when(categories.selectBatchIds(any())).thenReturn(List.of(category));
        when(relations.selectList(any())).thenReturn(List.of());

        List<GameResp> result = ReflectionTestUtils.invokeMethod(service, "toGameResps", List.of(game));

        assertNotNull(result);
        assertEquals("明日方舟", result.getFirst().getName());
        assertTrue(result.getFirst().getTags().isEmpty());
        verifyNoInteractions(tags);
    }
}
