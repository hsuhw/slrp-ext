grammar Problem;

import Tokens, Automaton, Transducer;

@header {
package generated;
}

problem
    : initialStatesRepr finalStatesRepr schedulerRepr processRepr invariantRepr relationRepr verifierOptions
    ;

initialStatesRepr
    : 'I0' automaton closedUnderTransFlag?
    ;

closedUnderTransFlag
    : 'closedUnderTransitions' ';'
    ;

finalStatesRepr
    : 'F' automaton
    ;

schedulerRepr
    : 'P1' transducer
    ;

processRepr
    : 'P2' transducer
    ;

invariantRepr
    : 'A' automaton
    ;

relationRepr
    : 'T' transducer
    ;

verifierOptions
    : (verifierOption ';')*
    ;

verifierOption
    : 'initAutomatonStateGuessing' ':' integerRange # unusedOption
    | 'automatonStateGuessing' ':' integerRange # invariantSearchSpace
    | 'transducerStateGuessing' ':' integerRange # relationSearchSpace
    | 'explicitChecksUntilLength' ':' INTEGER # unusedOption
    | 'symmetries' ':' symmetryOption (',' symmetryOption)* # unusedOption
    | 'closedUnderTransitions' # unusedOption
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
