package info.mengnan.aitalk.server.config;

import info.mengnan.aitalk.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.rag.service.DirectModelInvoker;
import info.mengnan.aitalk.server.content.DocumentEmbedding;
import info.mengnan.aitalk.server.service.ModelConfigService;
import info.mengnan.aitalk.kb.core.DocumentImageExtractor;
import info.mengnan.aitalk.kb.core.ImageTextGenerator;
import info.mengnan.aitalk.kb.core.SequentialDocumentExtractor;
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
    public DocumentEmbedding documentEmbedding(DynamicEmbeddingStoreRegistry embeddingStoreRegistry,
                                               ModelRegistry modelRegistry,
                                               ModelConfigService modelConfigService,
                                               DocumentImageExtractor documentImageExtractor,
                                               SequentialDocumentExtractor sequentialDocumentExtractor) {
        return new DocumentEmbedding(embeddingStoreRegistry, modelRegistry, modelConfigService,
                documentImageExtractor, sequentialDocumentExtractor);
    }

}
