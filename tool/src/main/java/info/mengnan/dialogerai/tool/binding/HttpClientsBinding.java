package info.mengnan.dialogerai.tool.binding;

import info.mengnan.dialogerai.common.http.HttpClients;

/**
 * 将 {@link HttpClients} 注册为 GraalJS 绑定，JS 中通过 {@code http} 访问。
 */
public class HttpClientsBinding extends HttpClients implements ContextBinding {

    @Override
    public String bindingName() {
        return "http";
    }
}
