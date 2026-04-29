package info.mengnan.aitalk.tool;

/**
 * 配置提供者接口，用于在 JS 脚本中读取数据库配置
 */
@FunctionalInterface
public interface ConfigProvider {

    /**
     * 获取明文配置值
     * @param configKey 配置键
     * @return 配置值，不存在返回 null
     */
    String getConfig(String configKey);
}