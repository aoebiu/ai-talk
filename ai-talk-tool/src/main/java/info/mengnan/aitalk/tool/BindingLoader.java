package info.mengnan.aitalk.tool;

import info.mengnan.aitalk.tool.binding.ContextBinding;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从配置文件加载 {@link ContextBinding} 实例，供 GraalJS Context 绑定使用。
 * 配置格式：每行一个全限定类名。
 * 若类实现了 {@link ContextBinding}，使用 {@code bindingName()} 作为绑定名；
 * 否则使用简单类名首字母小写作为绑定名。
 */
@Slf4j
public class BindingLoader {

    private BindingLoader() {}

    /**
     * 从 classpath 中加载绑定配置文件，返回绑定名 -> 实例的映射。
     *
     * @param configPath classpath 相对路径，例如 {@code META-INF/bindings_config}
     * @return 有序的绑定映射，加载失败的条目会被跳过
     */
    public static Map<String, Object> load(String configPath) {
        return load(configPath, null);
    }

    public static Map<String, Object> load(String configPath, Class<?> clazz) {
        Map<String, Object> result = new LinkedHashMap<>();
        InputStream is = null;

        if (clazz != null) {
            is = clazz.getResourceAsStream(configPath);
            if (is == null && !configPath.startsWith("/")) {
                is = clazz.getClassLoader().getResourceAsStream(configPath);
            }
        }

        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(configPath);
        }

        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream(configPath);
        }

        if (is == null) {
            log.warn("Bindings config not found: {}", configPath);
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> loadEntry(line, result));
        } catch (IOException e) {
            log.error("Failed to read bindings config: {}", e.getMessage(), e);
        }

        return result;
    }
    private static void loadEntry(String className, Map<String, Object> result) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            String name;
            if (instance instanceof ContextBinding binding) {
                name = binding.bindingName();
            } else {
                String simpleName = clazz.getSimpleName();
                name = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
            }
            result.put(name, instance);
            log.info("Loaded binding: {} -> {}", name, className);
        } catch (Exception e) {
            log.error("Failed to instantiate binding ({}): {}", className, e.getMessage());
        }
    }
}
