package info.mengnan.dialogerai.tool.binding;

/**
 * 标记接口：声明一个可绑定到 GraalJS Context 的对象。
 * 实现类通过覆写 {@link #bindingName()} 自定义 JS 中的变量名。
 */
public interface ContextBinding {

    /**
     * 绑定名称，即 JS 中访问该对象的变量名。
     */
    String bindingName();
}
