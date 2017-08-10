grammar Problem;

import Tokens, Automaton, Transducer;

@header {
package generated;
}

problem
    : initStatesRepr tgtStatesRepr schedulerRepr processRepr verifierOptions
    ;

initStatesRepr
    : 'I0' automaton closedUnderTransFlag?
    ;

closedUnderTransFlag
    : 'closedUnderTransitions' ';'
    ;

tgtStatesRepr
    : 'F' automaton
    ;

schedulerRepr
    : 'P1' transducer
    ;

processRepr
    : 'P2' transducer
    ;

verifierOptions
    : (verifierOption ';')*
    ;

verifierOption
    : 'initAutomatonStateGuessing' ':' integerRange
    | 'automatonStateGuessing' ':' integerRange
    | 'transducerStateGuessing' ':' integerRange
    | 'explicitChecksUntilLength' ':' INTEGER
    | 'symmetries' ':' symmetryOption (',' symmetryOption)*
    | 'closedUnderTransitions'
    | 'useRankingFunctions'
    | 'monolithicWitness'
    | 'noPrecomputedInvariant'
    | 'logLevel' ':' INTEGER
    | 'parallel' ':' INTEGER
    ;

integerRange
    : INTEGER '..' INTEGER
    ;

symmetryOption
    : 'rotation'
    | 'rotationStartingWith' '{' ID '}'
    ;
