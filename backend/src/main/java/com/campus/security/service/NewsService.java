package com.campus.security.service;

import com.campus.security.common.result.Result;
import com.campus.security.entity.News;

import java.util.List;

public interface NewsService {
    Result<List<News>> getNewsList();
    Result<News> getNewsDetail(Long id);
}
