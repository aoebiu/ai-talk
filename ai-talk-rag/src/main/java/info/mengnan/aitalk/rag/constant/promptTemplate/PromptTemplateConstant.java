package info.mengnan.aitalk.rag.constant.promptTemplate;

import dev.langchain4j.model.input.PromptTemplate;

// langchain4j默认的提示词可能不符合要求,无法返回需要的结果,在此处进行微调
public class PromptTemplateConstant {

    public static final PromptTemplate QUERY_ROUTER_PROMPT_TEMPLATE = PromptTemplate.from("""
             Based on the user query, determine the most suitable data source(s)
                to retrieve relevant information from the following options:
                {{options}}
            
                **Matching Rules:**
                1. If the query is RELATED to any data source's topic, select that data source
                2. "Related" means the query is about the same general subject area
                3. Choose data sources that could potentially contain information relevant to the query
                4. Be inclusive rather than exclusive in your matching
            
                It is very important that your answer consists of either a single number
                or multiple numbers separated by commas and nothing else!
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
}
