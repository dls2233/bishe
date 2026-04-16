package com.campus.security.task;

import com.campus.security.entity.News;
import com.campus.security.mapper.NewsMapper;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Random;

@Component
public class NewsSyncTask {

    @Autowired
    private NewsMapper newsMapper;

    // 定时任务：每隔1小时执行一次，启动后延迟5秒执行第一次
    @Scheduled(fixedRate = 3600000, initialDelay = 5000)
    public void syncNewsFromRSS() {
        System.out.println("开始同步网络新闻...");
        // 使用博客园的公开RSS源作为科技与安全资讯示例
        String rssUrl = "https://feed.cnblogs.com/blog/sitehome/rss"; 
        try {
            URL feedUrl = new URL(rssUrl);
            java.net.URLConnection connection = feedUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(connection));

            List<SyndEntry> entries = feed.getEntries();
            for (SyndEntry entry : entries) {
                String title = entry.getTitle();
                if (title == null || title.isEmpty()) continue;
                
                // 去重判断
                if (newsMapper.findByTitle(title) == null) {
                    News news = new News();
                    news.setTitle(title);
                    news.setCategory("科技前沿");
                    
                    // 提取内容
                    String desc = "";
                    if (entry.getDescription() != null) {
                        desc = entry.getDescription().getValue();
                    } else if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                        desc = entry.getContents().get(0).getValue();
                    } else {
                        desc = "该资讯暂无详细内容摘要，请点击原链接查看。";
                    }
                    
                    // 简单去除HTML标签
                    desc = desc.replaceAll("<[^>]+>", "");
                    // 如果内容过长，截取前200个字符
                    if (desc.length() > 200) {
                        desc = desc.substring(0, 200) + "...";
                    }
                    news.setContent(desc);
                    
                    // 随机配置一个占位封面
                    news.setCoverUrl(getRandomCover());
                    news.setViews(new Random().nextInt(1000));
                    
                    newsMapper.insert(news);
                    System.out.println("成功抓取网络新闻: " + title);
                }
            }
            System.out.println("网络新闻同步完成！");
        } catch (Exception e) {
            System.err.println("同步新闻失败: " + e.getMessage());
        }
    }

    private String getRandomCover() {
        String[] covers = {
            "https://images.unsplash.com/photo-1584036561566-baf8f5f1b144?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1541339907198-e08756dedf3f?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1563206767-5b18f218e8de?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1495020689067-958852a7765e?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=600&q=80"
        };
        return covers[new Random().nextInt(covers.length)];
    }
}