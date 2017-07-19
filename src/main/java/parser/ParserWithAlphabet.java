package parser;

public interface ParserWithAlphabet<T> extends Parser
{
    T getAlphabet();
}
