package info.mengnan.aitalk.rag.constant.promptTemplate;

import dev.langchain4j.model.input.PromptTemplate;

// langchain4j默认的提示词可能不符合要求,无法返回需要的结果,在此处进行微调
public class PromptTemplateConstant {

    public static final PromptTemplate QUERY_ROUTER_PROMPT_TEMPLATE = PromptTemplate.from("""
                Based on the user query, determine which data source(s) to retrieve from:
                {{options}}
            
                **Rules:**
                - Select data source ONLY if query is DIRECTLY related to its specific topic
                - "Related" means query is about the exact same subject matter
                - If no data source is relevant, return EMPTY (nothing, no text)
                - Output must be ONLY a single number, comma-separated numbers, or EMPTY
            
                User query: {{query}}
            """
    );


    public static final PromptTemplate COMPRESSION_PROMPT_TEMPLATE = PromptTemplate.from("""
            Please summarize and condense the following conversation to retain key information and context.
             The summary should be concise but contain important discussion points and conclusions.

            Conversation content:
            {{query}}

            Please provide a concise summary:
            """
    );

    public static final PromptTemplate CONTENT_INJECTOR_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                        {{userMessage}}

                        Use the following information to answer:
                        {{contents}}
                    """
    );

    public static final PromptTemplate TITLE_GENERATION_PROMPT_TEMPLATE = PromptTemplate.from("""
            Generate a concise title that summarizes the following content.
            The title must be in the same language as the provided content.
            Ensure the title is under 10 words and captures the main essence.
            
            Content:
            {{query}}
            
            Title:
            """
    );

    public static final PromptTemplate IDENTIFY_PICTURE_PROMPT_TEMPLATE = PromptTemplate.from("""
            Please analyze this image and describe in detail what you see.
            """
    );

    /**
     * 根据工具元数据与入参，仅生成可放入 execute 字段的脚本正文（无解释、无 Markdown）。
     * 变量：name, description, property, required, executeDraft, arguments
     */
    public static final PromptTemplate TOOL_EXECUTE_SCRIPT_GENERATION_PROMPT_TEMPLATE = PromptTemplate.from("""
            Output ONLY a raw JavaScript function starting with "function execute(params)".
            No explanations, no markdown fences, no module.exports.

            ## Available global: `http`
            - `http.get(url)` / `http.post(url, jsonBody)` / `http.put(url, jsonBody)` / `http.delete(url)`
            - All methods support an optional second (or third) `headers` parameter: e.g. `http.get(url, {"Authorization":"Bearer xxx"})`
            - All methods return a JSON **string** (not object):
              Success: `{"status":200, "body":"..."}`
              Error:   `{"error":true, "message":"...", "type":"..."}`
            - To access fields: `var resp = JSON.parse(http.get(url));`
            - The `body` field is also a string; parse again if the API returns JSON: `JSON.parse(resp.body)`

            ## Examples
            GET — direct return:
              function execute(params) {
                  return http.get('https://api.example.com/users/' + params.userId);
              }
            POST — with payload:
              function execute(params) {
                  var payload = JSON.stringify({ title: params.title, body: params.body });
                  return http.post('https://api.example.com/posts', payload);
              }
            GET — parse response for business logic:
              function execute(params) {
                  var resp = JSON.parse(http.get('https://api.example.com/posts?userId=' + params.userId));
                  var posts = JSON.parse(resp.body);
                  var count = Array.isArray(posts) ? posts.length : 0;
                  return JSON.stringify({ userId: params.userId, postCount: count });
              }

            ## Tool metadata
            Name: {{name}}
            Description: {{description}}
            Parameter schema: {{property}}
            Required params: {{required}}

            ## Requirements
            - Read inputs from `params` only; use `||` for optional defaults.
            - Prefer `http` for real API calls; only use in-memory mock when no real endpoint exists.
            - Check `error` field in parsed HTTP responses before accessing `body`.
            - If a draft is provided, refine it; otherwise create from scratch.
            - Return user-facing Chinese or bilingual strings when appropriate.

            Script:
            """
    );
}
