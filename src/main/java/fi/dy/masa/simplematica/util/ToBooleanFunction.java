package fi.dy.masa.simplematica.util;

@FunctionalInterface
public interface ToBooleanFunction<R>
{
    boolean applyAsBoolean(R value);
}
