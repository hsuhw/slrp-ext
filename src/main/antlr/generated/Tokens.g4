lexer grammar Tokens;

fragment
DIGIT
    : [0-9]
    ;

fragment
LETTER
    : [a-zA-Z]
    ;

ID
    : (LETTER | DIGIT | '_')* (LETTER | '_') DIGIT*
    ;

INTEGER
    : DIGIT+
    ;

LINE_COMMENT
    : '//' .*? '\r'? '\n' -> skip
    ;

COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
