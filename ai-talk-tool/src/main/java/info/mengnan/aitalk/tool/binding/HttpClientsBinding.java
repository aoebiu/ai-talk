package info.mengnan.aitalk.tool.binding;

import info.mengnan.aitalk.common.http.HttpClients;

/**
 * 将 {@link HttpClients} 注册为 GraalJS 绑定，JS 中通过 {@code http} 访问。
 */
public class HttpClientsBinding extends HttpClients implements ContextBinding {

    @Override
    public String bindingName() {
        return "http";
    }
}
