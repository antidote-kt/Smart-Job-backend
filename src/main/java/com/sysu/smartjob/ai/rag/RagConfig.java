package com.sysu.smartjob.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RagConfig {
    @Resource
    private EmbeddingModel embeddingModel;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore){
        List<Document> documents = new ArrayList<>();
        
        try {
            URL docsUrl = getClass().getClassLoader().getResource("docs");
            if (docsUrl != null) {
                Path docsPath = Paths.get(docsUrl.toURI());
                documents = FileSystemDocumentLoader.loadDocuments(docsPath);
                System.out.println("成功加载 " + documents.size() + " 个文档");
            }
        } catch (Exception e) {
            System.out.println("无法加载docs文档: " + e.getMessage());
        }
        
        // 文档切割
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 200);
        // 向量化并存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()))
                .documentSplitter(splitter)
                .build();
        
        // 只有在有文档时才进行摄取
        if (!documents.isEmpty()) {
            ingestor.ingest(documents);
        }
        
        // 构建检索器
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.75)
                .build();
        return contentRetriever;
    }
}
