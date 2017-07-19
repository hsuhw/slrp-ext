grammar Transducer;

import Tokens, Automaton;

transducer
    : ID? '{' startStates transducerTransitions acceptStates '}'
    ;

transducerTransitions
    : (transducerTransition ';')*
    ;

transducerTransition
    : ID '->' ID '[' transducerTransitionLabel ']'
    | ID '->' ID transducerTransitionLabel
    ;

transducerTransitionLabel
    : epsilonTransitionLabel
    | monadIOTransitionLabel
    ;

monadIOTransitionLabel
    : (ID '/' ID)?
    ;
