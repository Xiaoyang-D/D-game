package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.SearchQueryReq;
import com.xiaoyang.d_game.dto.SearchResp;

public interface SearchService {

    SearchResp search(SearchQueryReq req);
}
