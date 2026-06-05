package info.mengnan.dialogerai.rag.container.factory;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 通用模型工厂实现
 * 从 META-INF/model-mapping/*.properties 配置文件中加载 Provider 映射，
 * 运行时通过反射创建模型实例。
 *
 * 新增 Provider 只需在 classpath 中添加对应的 properties 文件，无需编写 Java 代码。
 */
@Slf4j
public class CapableModelFactory implements ChatModelFactory,
                                            EmbeddingModelFactory,
                                            ScoringModelFactory,
                                            ModerationModelFactory,
                                            ImageModelFactory {

    private static final String MAPPING_LOCATION = "META-INF/model-mapping/";

    private final Map<String, Map<ModelType, String>> modelClassMapping = new HashMap<>();

    public CapableModelFactory() {
        loadMappings();
    }

    private void loadMappings() {
        try {
            Thread.currentThread().getContextClassLoader().getResources(MAPPING_LOCATION);

        } catch (IOException e) {
            log.warn("Failed to scan model-mapping directory", e);
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> allResources = classLoader.getResources("META-INF/model-mapping");

            while (allResources.hasMoreElements()) {
                URL dirUrl = allResources.nextElement();
                loadFromDirectory(dirUrl, classLoader);
            }
        } catch (IOException e) {
            log.error("Failed to load model mapping configurations", e);
        }
    }

    private void loadFromDirectory(URL dirUrl, ClassLoader classLoader) {
        String protocol = dirUrl.getProtocol();

        if ("file".equals(protocol)) {
            loadFromFileSystem(dirUrl);
        } else if ("jar".equals(protocol)) {
            loadFromJar(dirUrl, classLoader);
        }
    }

    private void loadFromFileSystem(URL dirUrl) {
        java.io.File dir = new java.io.File(dirUrl.getPath());
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".properties"));
            if (files != null) {
                for (java.io.File file : files) {
                    try (InputStream is = file.toURI().toURL().openStream()) {
                        loadSingleMapping(is, file.getName());
                    } catch (IOException e) {
                        log.warn("Failed to load mapping file: {}", file.getName(), e);
                    }
                }
            }
        }
    }

    private void loadFromJar(URL dirUrl, ClassLoader classLoader) {
        try (JarFile jar = openJarFile(dirUrl)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(MAPPING_LOCATION) && name.endsWith(".properties") && !entry.isDirectory()) {
                    try (InputStream is = classLoader.getResourceAsStream(name)) {
                        if (is != null) {
                            loadSingleMapping(is, name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load mappings from JAR: {}", dirUrl, e);
        }
    }

    private JarFile openJarFile(URL jarUrl) throws IOException {
        JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
        connection.setUseCaches(false);
        return connection.getJarFile();
    }

    private void loadSingleMapping(InputStream is, String source) throws IOException {
        Properties props = new Properties();
        props.load(is);

        String providerCode = props.getProperty("providerCode");
        if (providerCode == null || providerCode.isBlank()) {
            log.warn("Skipping mapping file without providerCode: {}", source);
            return;
        }

        Map<ModelType, String> typeMapping = new HashMap<>();
        for (ModelType modelType : ModelType.values()) {
            String className = props.getProperty(modelType.name());
            if (className != null && !className.isBlank()) {
                typeMapping.put(modelType, className.trim());
            }
        }

        modelClassMapping.put(providerCode, typeMapping);
        log.info("Loaded model mapping for provider: {} from {} (types: {})",
                providerCode, source, typeMapping.keySet());
    }

    @Override
    public Object createModel(ModelConfig modelConfig, ModelType modelType) {
        try {
            String providerCode = modelConfig.getModelProvider();

            Map<ModelType, String> providerModels = modelClassMapping.get(providerCode);
            if (providerModels == null) {
                throw new UnsupportedOperationException(
                        "Unsupported model provider: " + providerCode);
            }

            String className = providerModels.get(modelType);
            if (className == null) {
                throw new UnsupportedOperationException(
                        "Model type '" + modelType + "' is not supported for provider: " + providerCode);
            }

            // 动态加载类
            Class<?> modelClass = Class.forName(className);

            // 获取 builder() 方法
            Method builderMethod = modelClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();

            // 设置 apiKey（如果该方法存在）
            try {
                Method apiKeyMethod = builderClass.getMethod("apiKey", String.class);
                builder = apiKeyMethod.invoke(builder, modelConfig.getApiKey());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have apiKey method, skipping", className);
            }

            // 设置 modelName（如果该方法存在）
            try {
                Method modelNameMethod = builderClass.getMethod("modelName", String.class);
                builder = modelNameMethod.invoke(builder, modelConfig.getModelName());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have modelName method, skipping", className);
            }

            // 调用 build() 方法
            Method buildMethod = builderClass.getMethod("build");
            Object model = buildMethod.invoke(builder);

            log.info("Successfully created {} model: {} (provider: {}, modelName: {})",
                    modelType, className, providerCode, modelConfig.getModelName());

            return model;
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Model class not found in classpath. Please add the corresponding dependency for provider: "
                            + modelConfig.getModelProvider(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create model for provider: " + modelConfig.getModelProvider()
                            + ", type: " + modelType, e);
        }
    }
}
