package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.entity.News;
import com.campus.security.service.NewsService;
import com.campus.security.task.NewsSyncTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;
    
    @Autowired
    private NewsSyncTask newsSyncTask;

    @GetMapping("/list")
    public Result<List<News>> list() {
        return newsService.getNewsList();
    }

    @GetMapping("/{id}")
    public Result<News> detail(@PathVariable Long id) {
        return newsService.getNewsDetail(id);
    }
    
    @GetMapping("/sync")
    public Result<String> sync() {
        newsSyncTask.syncNewsFromRSS();
        return Result.success("同步触发成功");
    }
}
