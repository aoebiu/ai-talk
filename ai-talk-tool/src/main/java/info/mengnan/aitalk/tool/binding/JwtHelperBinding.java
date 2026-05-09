package info.mengnan.aitalk.tool.binding;

import info.mengnan.aitalk.common.crypto.JwtHelper;

/**
 * 将 {@link JwtHelper} 注册为 GraalJS 绑定，JS 中通过 {@code jwt} 访问。
 */
public class JwtHelperBinding extends JwtHelper implements ContextBinding {

    @Override
    public String bindingName() {
        return "jwt";
    }
}
