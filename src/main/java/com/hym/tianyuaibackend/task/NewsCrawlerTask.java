package com.hym.tianyuaibackend.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hym.tianyuaibackend.entity.SysNews;
import com.hym.tianyuaibackend.mapper.SysNewsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class NewsCrawlerTask {

    // Python 爬虫服务的地址
    private static final String PYTHON_CRAWL_BASE_URL = "http://127.0.0.1:8000/crawl/news";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SysNewsMapper sysNewsMapper;

    /**
     * 定时抓取农业资讯
     * cron = "0 0 2 * * ?" 表示每天凌晨 2 点 0 分 0 秒执行
     */
    @Scheduled(cron = "0 0 8,10,12,15,18 * * ?")
    public void executeCrawlTask() {
        log.info("【定时任务】开始抓取农业资讯...");
        fetchAndSaveNews();
        log.info("【定时任务】农业资讯抓取完成！");
    }

    /**
     * 核心的抓取与保存逻辑 (抽离出来方便后续手动触发测试)
     */
    public String fetchAndSaveNews() {
        int successCount = 0;
        int page = 0;
        boolean continueCrawling = true;

        List<SysNews> pendingInsertList = new ArrayList<>();

        log.info("=== 开始执行资讯爬取任务 ===");

        try {
            while (continueCrawling) {
                log.info("正在抓取第 {} 页数据...", page);

                // 1. 动态拼接请求 URL
                String requestUrl = PYTHON_CRAWL_BASE_URL + "?page=" + page + "&limit=20";
                String responseStr;

                // 将请求单独 try-catch，捕获到 404 视为翻到最后一页，优雅退出
                try {
                    responseStr = restTemplate.getForObject(requestUrl, String.class);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("404")) {
                        log.info("第 {} 页返回 404，已到达最后一页，抓取正常结束。", page);
                    } else {
                        log.warn("第 {} 页请求发生异常: {}，停止抓取。", page, e.getMessage());
                    }
                    break;
                }

                JSONObject responseJson = JSON.parseObject(responseStr);

                if (responseJson == null || responseJson.getInteger("code") != 200) {
                    log.error("爬虫接口返回异常，停止抓取: {}", responseStr);
                    break;
                }

                // 2. 解析返回的新闻列表
                JSONArray dataList = responseJson.getJSONArray("data");

                // 如果当前页没有数据了 (爬到了最后一页的末尾)，退出循环
                if (dataList == null || dataList.isEmpty()) {
                    log.info("第 {} 页没有数据，所有历史资讯抓取完毕！", page);
                    break;
                }

                // 3. 遍历并保存到 MySQL
                for (int i = 0; i < dataList.size(); i++) {
                    JSONObject newsObj = dataList.getJSONObject(i);
                    String sourceId = newsObj.getString("source_id");

                    // 防重校验
                    QueryWrapper<SysNews> query = new QueryWrapper<>();
                    query.eq("source_id", sourceId);
                    boolean exists = sysNewsMapper.exists(query);

                    if (exists) {
                        log.info("发现数据库中已存在文章 ID [{}], 增量抓取到此结束。", sourceId);
                        continueCrawling = false;
                        break;
                    }

                    // 组装实体类并入库
                    SysNews sysNews = new SysNews();
                    sysNews.setTitle(newsObj.getString("title"));
                    sysNews.setCoverImg(newsObj.getString("cover_img"));
                    sysNews.setDescription(newsObj.getString("description"));
                    sysNews.setContentMd(newsObj.getString("content_md"));
                    sysNews.setSourceUrl(newsObj.getString("source_url"));
                    sysNews.setSourceId(sourceId);
                    sysNews.setAuthor(newsObj.getString("author"));
                    sysNews.setIsCrawled(1);

                    // 处理 Tags
                    JSONArray tagsArray = newsObj.getJSONArray("tags");
                    if (tagsArray != null && !tagsArray.isEmpty()) {
                        sysNews.setTags(tagsArray.toJavaList(String.class));
                    } else {
                        sysNews.setTags(null);
                    }

                    String createTimeStr = newsObj.getString("create_time");
                    LocalDateTime createTime = parseIssueTime(createTimeStr);
                    sysNews.setCreateTime(createTime);
                    String updateTimeStr = newsObj.getString("issue_time");
                    LocalDateTime updateTime = parseIssueTime(updateTimeStr);
                    sysNews.setUpdateTime(updateTime);

                    // 初始化统计数据
                    sysNews.setViewCount(0);
                    sysNews.setLikeCount(0);
                    sysNews.setDislikeCount(0);
                    sysNews.setFavoriteCount(0);
                    sysNews.setCommentCount(0);
                    sysNews.setIsDeleted(0);

                    pendingInsertList.add(sysNews);
                }

                // 如果 continueCrawling 在 for 循环里被设为 false 了，外层的 while 也会跟着停止
                if (!continueCrawling) {
                    break;
                }

                // 4. 翻页，并休眠一小会儿防止被封 IP (非常重要！)
                page++;
                try {
                    // 每次翻页休息 1.5 秒
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("抓取环节结束，正在按照时间对 {} 条数据进行排序...", pendingInsertList.size());
            pendingInsertList.sort(Comparator.comparing(SysNews::getCreateTime));

            // 2. 遍历排序后的列表，依次写入数据库
            for (SysNews news : pendingInsertList) {
                sysNewsMapper.insert(news);
                successCount++;
                log.debug("成功入库文章: {}", news.getTitle());
            }

            String resultMsg = String.format("任务结束。本次共翻了 %d 页，排序并新增入库 %d 条资讯。", page, successCount);
            log.info("=== {} ===", resultMsg);
            return resultMsg;

        } catch (Exception e) {
            log.error("抓取入库过程中发生异常", e);
            return "执行异常中断: " + e.getMessage();
        }
    }

    /**
     * 辅助方法：解析网页传来的字符串时间
     * 兼容 "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss" 等情况
     */
    private LocalDateTime parseIssueTime(String issueTimeStr) {
        if (issueTimeStr == null || issueTimeStr.trim().isEmpty()) {
            return LocalDateTime.now(); // 没有时间则默认当前时间
        }

        try {
            issueTimeStr = issueTimeStr.trim();
            // 农民日报一般格式是 "2023-10-18 10:30" (长度 16)
            if (issueTimeStr.length() == 16) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                return LocalDateTime.parse(issueTimeStr, formatter);
            }
            // 如果带了秒数 "2023-10-18 10:30:00"
            else if (issueTimeStr.length() == 19) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(issueTimeStr, formatter);
            }
            // 如果只有日期 "2023-10-18"
            else if (issueTimeStr.length() == 10) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");
                return LocalDateTime.parse(issueTimeStr + " 00:00:00", formatter);
            }

            // 无法精确匹配的，直接用当前时间兜底
            log.warn("未能识别的日期格式: [{}], 使用当前时间兜底", issueTimeStr);
            return LocalDateTime.now();
        } catch (Exception e) {
            log.error("解析新闻发布时间 [{}] 失败，使用当前时间代替。错误: {}", issueTimeStr, e.getMessage());
            return LocalDateTime.now();
        }
    }
}