package com.sysu.smartjob.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class InterviewQuestionTool {

    @Tool("在面试鸭网站搜索面试题，返回相关的面试题目和答案")
    public String searchInterviewQuestions(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String searchUrl = "https://www.mianshiya.com/search/all?searchText=" + encodedKeyword;

        log.info("搜索面试题，关键词: {}, URL: {}", keyword, searchUrl);

        Document doc = null;
        try {
            doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();
        } catch (IOException e) {
            log.info("get web error", e);
            return e.getMessage();
        }

        List<String> questions = new ArrayList<>();

        // 选择面试题列表
        Elements questionElements = doc.select(".ant-table-cell > a");
        questionElements.forEach(element -> questions.add(element.text()));
        return String.join("\n", questions) ;
    }


}