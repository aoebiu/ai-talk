package info.mengnan.dialogerai.server.config;

import info.mengnan.dialogerai.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import info.mengnan.dialogerai.server.core.DocumentEmbedding;
import info.mengnan.dialogerai.kb.core.DocumentImageExtractor;
import info.mengnan.dialogerai.kb.core.ImageTextGenerator;
import info.mengnan.dialogerai.kb.core.SequentialDocumentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KbConfiguration {

    @Bean
    public ImageTextGenerator imageTextGenerator(DirectModelInvoker directModelInvoker) {
        return directModelInvoker::imageToText;
    }

    @Bean
    public DocumentImageExtractor documentImageExtractor(ImageTextGenerator imageTextGenerator) {
        return new DocumentImageExtractor(imageTextGenerator);
    }

    @Bean
    public SequentialDocumentExtractor sequentialDocumentExtractor(ImageTextGenerator imageTextGenerator) {
        return new SequentialDocumentExtractor(imageTextGenerator);
    }

    @Bean
    public DocumentEmbedding documentEmbedding(DynamicEmbeddingStoreRegistry embeddingStoreRegistry) {
        return new DocumentEmbedding(embeddingStoreRegistry);
    }

}
