package com.campus.security.service.impl;

import com.campus.security.common.result.Result;
import com.campus.security.entity.News;
import com.campus.security.mapper.NewsMapper;
import com.campus.security.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsMapper newsMapper;

    @Override
    public Result<List<News>> getNewsList() {
        return Result.success(newsMapper.findAll());
    }

    @Override
    public Result<News> getNewsDetail(Long id) {
        News news = newsMapper.findById(id);
        if (news != null) {
            newsMapper.incrementViews(id);
            news.setViews(news.getViews() + 1);
            return Result.success(news);
        }
        return Result.error(404, "资讯不存在");
    }
}
