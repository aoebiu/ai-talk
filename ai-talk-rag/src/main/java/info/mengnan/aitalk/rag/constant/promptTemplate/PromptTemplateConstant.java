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
}
