package util;

import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;

public final class Predicates
{
    public static BooleanBooleanPredicate AND = (a, b) -> a && b;
    public static BooleanBooleanPredicate OR = (a, b) -> a || b;
}
