grammar Automaton;

import Tokens;

automaton
    : ID? '{' startStates transitions acceptStates '}'
    ;

startStates
    : 'start' ':' stateList ';'
    | 'init' ':' stateList ';'
    ;

acceptStates
    : 'accept' ':' stateList ';'
    | 'acc' ':' stateList ';'
    | 'accepting' ':' stateList ';'
    ;

transitions
    : (transition ';')*
    ;

transition
    : ID '->' ID '[' transitionLabel ']'
    | ID '->' ID transitionLabel
    ;

transitionLabel
    : epsilonTransitionLabel
    | monadTransitionLabel
    | pairTransitionLabel
    ;

epsilonTransitionLabel
    :
    ;

monadTransitionLabel
    : ID
    ;

pairTransitionLabel
    : ID ',' ID
    ;

stateList
    : ID (',' ID)*
    ;
