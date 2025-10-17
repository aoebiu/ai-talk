package info.mengnan.aitalk.rag.tools;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ToolDescription {

    private String name;
    private String description;
    private Map<String, String> property;
    private List<String> required;

    private String execute;
}
