grammar Problem;

import Tokens, Automaton, Transducer;

@header {
package generated;
}

problem
    : initialConfigs finalConfigs scheduler process invariant? order? verifierOptions
    ;

initialConfigs
    : 'I0' automaton closedUnderTransFlag?
    ;

closedUnderTransFlag
    : 'closedUnderTransitions' ';'
    ;

finalConfigs
    : 'F' automaton
    ;

scheduler
    : 'P1' transducer
    ;

process
    : 'P2' transducer
    ;

invariant
    : 'Given A' automaton
    ;

order
    : 'Given T' transducer
    ;

verifierOptions
    : (verifierOption ';')*
    ;

verifierOption
    : 'initAutomatonStateGuessing' ':' integerRange # unusedOption
    | 'automatonStateGuessing' ':' integerRange # invariantSizeBound
    | 'transducerStateGuessing' ':' integerRange # orderSizeBound
    | 'explicitChecksUntilLength' ':' INTEGER # unusedOption
    | 'symmetries' ':' symmetryOption (',' symmetryOption)* # unusedOption
    | 'closedUnderTransitions' # closedUnderTrans
    | 'useRankingFunctions' # unusedOption
    | 'monolithicWitness' # unusedOption
    | 'noPrecomputedInvariant' # unusedOption
    | 'logLevel' ':' INTEGER # unusedOption
    | 'parallel' ':' INTEGER # unusedOption
    ;

integerRange
    : INTEGER '..' INTEGER
    ;

symmetryOption
    : 'rotation'
    | 'rotationStartingWith' '{' ID '}'
    ;
