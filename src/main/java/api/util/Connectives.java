package api.util;

import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;

public interface Connectives
{
    BooleanBooleanPredicate AND = (a, b) -> a && b;
    BooleanBooleanPredicate OR = (a, b) -> a || b;
}
