package info.mengnan.dialogerai.rag.constant.promptTemplate;

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

    /** 注入分隔符：用于在保存时将原始用户消息从注入内容中剥离 */
    public static final String CONTENT_INJECTION_SEPARATOR = "\n\nUse the following information to answer:\n";

    public static final PromptTemplate CONTENT_INJECTOR_PROMPT_TEMPLATE = PromptTemplate.from(
            "{{userMessage}}" + CONTENT_INJECTION_SEPARATOR + "{{contents}}"
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
     * 分析用户提示词，判断工具需要哪些运行时能力（HTTP请求、配置读取等）
     * 变量：prompt
     */
    public static final PromptTemplate TOOL_CAPABILITY_ANALYSIS_PROMPT_TEMPLATE = PromptTemplate.from("""
            You are a capability analyzer. Based on the user's prompt, determine which runtime capabilities the tool needs.
            Output ONLY valid JSON, no explanations, no markdown fences.

            ## Available capabilities
            - **HTTP**: The tool needs to make HTTP requests (call external APIs, fetch remote data, webhooks, etc.)
            - **Config**: The tool needs to read runtime configuration (API keys, base URLs, secrets, tokens, credentials, etc.)
            - **JWT**: The tool needs to generate JSON Web Tokens (JWT signing, token creation, EdDSA/Ed25519 authentication, etc.)

            ## Rules
            - If the tool needs to call any external API or fetch remote data, set `needsHttp` to true.
            - If the tool needs any secret, API key, token, base URL, or runtime configuration that should not be hardcoded, set `needsConfig` to true.
            - If the tool needs to generate or sign JWT tokens, set `needsJwt` to true.
            - A tool that calls external APIs almost always needs both HTTP and Config (for API keys/URLs).
            - A tool that generates JWT tokens almost always needs Config (for private keys).
            - A pure computation tool (math, string processing, data transformation) needs neither.

            ## Output format
            {"needsHttp": true/false, "needsConfig": true/false, "needsJwt": true/false}

            ## User prompt
            {{prompt}}

            JSON:
            """
    );


    /**
     * 根据用户提示词，生成工具的名称、描述、属性列表（含是否必填）和执行脚本
     * 变量：prompt
     */
    public static final PromptTemplate TOOL_METADATA_GENERATION_PROMPT_TEMPLATE = PromptTemplate.from("""
            You are a tool metadata generator. Based on the user's prompt, generate a JSON object describing a tool.
            Output ONLY valid JSON, no explanations, no markdown fences.

            ## Output format
            {
              "name": "tool_name_in_snake_case",
              "description": "A clear description of what this tool does, used by LLM to decide when to select this tool",
              "properties": {
                "param1": {
                  "type": "string",
                  "description": "Description of param1"
                },
                "param2": {
                  "type": "number",
                  "description": "Description of param2"
                }
              },
              "required": ["param1"],
              "execute": "function execute(params) { ... }"
            }

            ## Rules
            - `name`: a concise, snake_case identifier for the tool.
            - `description`: a clear, specific sentence describing the tool's purpose. This is the primary basis for the LLM to choose this tool, so it must accurately reflect the tool's capability.
            - `properties`: each parameter has `type` (string/number/boolean/array/object) and `description`. Add all parameters the tool logically needs.
            - `required`: list only parameters that are essential for the tool to function. Optional parameters should NOT appear here.
            - `execute`: the value must be a raw JavaScript function starting with `function execute(params)`. No explanations, no markdown fences, no module.exports. Read inputs from `params` only; use `||` for optional defaults. Return a JSON string as the result.
            - Infer reasonable parameters from the user's description. If the user mentions specific inputs, include them.
            - Use Chinese for description fields when the user's prompt is in Chinese.

            ## Runtime Environment Constraints (CRITICAL)
            The execute script runs in GraalJS environment (JavaScript on the JVM, ECMAScript-compliant).
            - Do not use require() unless explicitly noted
            - Do not assume Node.js globals like process, Buffer, __dirname, or require.cache.
            - Do not use Web/Browser API globals that are unavailable in GraalJS, including but not limited to: btoa, atob, TextEncoder, TextDecoder, fetch, URL, URLSearchParams, setTimeout, setInterval.

            ## User prompt
            {{prompt}}

            JSON:
            """
    );

    /**
     * 根据工具执行脚本生成批量测试用例（JSON 数组）
     * 变量：count, toolName, toolDescription, executeScript, toolProperty, toolRequired
     */
    public static final PromptTemplate TOOL_CASE_GENERATION = PromptTemplate.from("""
            You are a test case generator. Analyze the execute script below to determine which params.* fields are actually read, then generate {{count}} diverse and realistic test cases.
            Output ONLY a valid JSON array, no markdown fences, no explanations.

            ## Tool
            Name: {{toolName}}
            Description: {{toolDescription}}

            Official properties (JSON object: parameter name -> description): {{toolProperty}}
            Required parameter names (JSON array of strings): {{toolRequired}}

            Execute script:
            ```javascript
            {{executeScript}}
            ```

            ## Output format
            [
              { "params": { "paramName": "value" }, "expected": "optional expected substring" },
              { "params": { "paramName": "value" } }
            ]

            ## Rules
            - Read the execute script carefully. Only include in "params" the fields that the script actually accesses via `params.*` or `params["*"]`.
            - For every param key you output, it MUST match the exact spelling of a key from "Official properties" when that property corresponds to a field the script reads. Do not rename or typo schema keys.
            - If the script reads a field that is not listed in Official properties, still include it only if it appears in the script (use the exact identifier from the script).
            - If the script does not read any field from `params`, set "params" to an empty object `{}`.
            - Do NOT invent parameters based only on the tool name or description — script-accessed fields take precedence; use Official properties for correct key names when they align.
            - Generate exactly {{count}} test case objects with realistic, varied values. Cover required parameters with non-empty values in most cases.
            - The "expected" field is optional. Include it only when an expected output substring is reasonably predictable.
            - Match the language of the tool description (use Chinese if description is in Chinese).

            JSON array:
            """
    );

    // 能力片段提示词

    /**
     * HTTP 能力说明片段
     */
    public static final String TOOL_HTTP_CAPABILITY_SNIPPET = """

            ## Available: `http` object (global, pre-injected)
            - `http.get(url)` → returns JSON string `{"status":200,"body":"..."}`
            - `http.get(url, headers)` → same, with custom headers (Java Map)
            - `http.post(url, body)` / `http.post(url, body, headers)` → same format
            - `http.put(url, body)` / `http.put(url, body, headers)` → same format
            - `http.delete(url)` / `http.delete(url, headers)` → same format
            - Response is always a JSON string. Use `JSON.parse(http.get(url))` to access `.status` and `.body`.
            - headers example: `{"Authorization": "Bearer " + token, "Content-Type": "application/json"}`
            """;

    /**
     * Config 能力说明片段
     */
    public static final String TOOL_CONFIG_CAPABILITY_SNIPPET = """

            ## Available: `config` object (global, pre-injected)
            - `config.getConfig(key)` → returns the config value as a string, or null if not found.
            - Use this to read API keys, base URLs, tokens, credentials, or any secret. **Never hardcode secrets in the script.**
            - If the config value is a JSON string, use `JSON.parse(config.getConfig(key))` to access its fields.

            ### Parameter Design Principle (CRITICAL)
            - Secrets and configuration (API keys, tokens, base URLs, credentials) must be read via `config.getConfig()` inside the execute function body.
            - **NEVER** add configuration fields to `properties` or `required`. Only user-provided runtime inputs (e.g., city name, query text) belong in `properties`.

            ### Example
            User prompt: "Query weather API, read API key from config"

            CORRECT — only runtime input as parameter, config read in execute():
              "properties": { "city": {"type": "string", "description": "City name"} }
              "required": ["city"]
              execute: var apiKey = config.getConfig('weather_api_key'); var city = params.city; ...

            WRONG — config field exposed as parameter:
              "properties": { "city": {...}, "apiKey": {"type": "string", "description": "Weather API key"} }
            """;

    /**
     * JWT 能力说明片段
     */
    public static final String TOOL_JWT_CAPABILITY_SNIPPET = """

            ## Available: `jwt` object (global, pre-injected)
            - `jwt.encode(payloadJson, key, algorithm, headersJson)` → returns JWT string (header.payload.signature)
            - `payloadJson`: JSON string containing claims, e.g. `'{"sub":"PROJECT_ID","iat":1234567890,"exp":1234568790}'`
            - `key`: the signing key. For HMAC (HS256/HS384/HS512) pass the raw secret string; for RSA/EC/EdDSA pass a Base64-encoded PKCS#8 private key.
            - `algorithm`: signing algorithm string. Supported values:
              - HMAC: `"HS256"`, `"HS384"`, `"HS512"` (symmetric key)
              - RSA: `"RS256"`, `"RS384"`, `"RS512"` (asymmetric key)
              - RSA-PSS: `"PS256"`, `"PS384"`, `"PS512"` (asymmetric key)
              - EC: `"ES256"`, `"ES384"`, `"ES512"` (asymmetric key)
              - EdDSA: `"EdDSA"` (asymmetric key, Ed25519)
            - `headersJson`: JSON string with extra JWT headers, e.g. `'{"kid":"KEY_ID"}'`. Can be null.

            ### Example 1: EdDSA (asymmetric key)
            ```
            var privateKey = config.getConfig('jwt_private_key');
            var kid = config.getConfig('jwt_key_id');
            var projectId = config.getConfig('jwt_project_id');
            var now = Math.floor(Date.now() / 1000);
            var payload = JSON.stringify({"sub": projectId, "iat": now - 30, "exp": now + 900});
            var headers = JSON.stringify({"kid": kid});
            var token = jwt.encode(payload, privateKey, "EdDSA", headers);
            ```

            ### Example 2: HMAC (symmetric key)
            ```
            var secret = config.getConfig('jwt_secret');
            var now = Math.floor(Date.now() / 1000);
            var payload = JSON.stringify({"sub": "user123", "iat": now, "exp": now + 3600});
            var token = jwt.encode(payload, secret, "HS256", null);
            ```
            """;
}
