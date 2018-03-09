grammar Automaton;

import Tokens;

automaton
    : ID? '{' startStates transitions acceptStates '}'
    ;

startStates
    : 'start' ':' states ';'
    | 'init' ':' states ';'
    ;

acceptStates
    : 'accept' ':' states ';'
    | 'acc' ':' states ';'
    | 'accepting' ':' states ';'
    ;

transitions
    : (transition ';')*
    ;

transition
    : ID '->' ID '[' transitionLabel ']'
    | ID '->' ID transitionLabel
    ;

transitionLabel
    : emptyLabel
    | simpleLabel
    | pairLabel
    ;

emptyLabel
    :
    ;

simpleLabel
    : ID
    ;

pairLabel
    : ID ',' ID
    ;

states
    : ID (',' ID)*
    ;
