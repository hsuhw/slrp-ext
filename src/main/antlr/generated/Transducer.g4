grammar Transducer;

import Tokens, Automaton;

transducer
    : ID? '{' startStates inOutTransitions acceptStates '}'
    ;

inOutTransitions
    : (inOutTransition ';')*
    ;

inOutTransition
    : ID '->' ID '[' inOutTransitionLabel ']'
    | ID '->' ID inOutTransitionLabel
    ;

inOutTransitionLabel
    : emptyLabel
    | slashedLabel
    ;

slashedLabel
    : ID '/' ID
    ;
