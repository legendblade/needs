package org.winterblade.minecraft.mods.needs.api;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JsonAdapter(ExpressionContext.Deserializer.class)
public abstract class ExpressionContext implements IExpression {
    // Ensure default constructor?
    protected ExpressionContext() {}

    IExpression expression;

    /**
     * Generates an expression context that always returns the given value
     * @param ctx    The context to make constant
     * @param amount The value to return
     * @param <T>    The type of the context
     * @return  The updated context
     */
    public static <T extends ExpressionContext> T makeConstant(final T ctx, final double amount) {
        ctx.expression = new ConstantAdjustmentWrappedExpression(amount);
        return ctx;
    }


    @Override
    public Double get() {
        return expression.get();
    }

    public boolean isRequired(final String arg) {
        return expression.isRequired(arg);
    }

    @Override
    public void setIfRequired(final String arg, final Supplier<Double> value) {
        expression.setIfRequired(arg, value);
    }

    protected abstract List<String> getElements();

    private IExpression deserializeExpression(final JsonElement json, final List<String> elements) {
        return deserializeExpression(
            json,
            elements
                .stream()
                .map((a) -> new Argument(a, 0))
                .toArray(Argument[]::new)
        );
    }

    private IExpression deserializeExpression(final JsonElement json, final Argument[] elements) {
        if (!json.isJsonPrimitive()) throw new JsonParseException("Expression must be a string or an integer");

        final JsonPrimitive primitive = json.getAsJsonPrimitive();

        if(primitive.isNumber()) {
            return new ConstantAdjustmentWrappedExpression(primitive.getAsDouble());
        }

        // Going to a primitive then a string avoids string builder'ing it all
        final String exp = json.getAsJsonPrimitive().getAsString().trim();
        final Expression expression = new Expression(exp, elements);

        if (!expression.checkSyntax()) {
            throw new JsonParseException(
                "Unable to parse expression '" + exp + "', valid variables in this context are: "
                + String.join(", ",
                    Arrays
                        .stream(elements)
                        .map(Argument::getArgumentName)
                        .collect(Collectors.toList())
                ) + "\n" + expression.getErrorMessage()
            );
        }

        return elements.length <= 0
                ? new ParameterlessParsedWrappedExpression(expression)
                : new ParsedWrappedExpression(expression, elements);
    }

    public static class Deserializer implements JsonDeserializer<ExpressionContext> {

        @Override
        public ExpressionContext deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final ExpressionContext output;
            final Class<?> clazz = TypeToken.of(typeOfT).getRawType();

            try {
                output = (ExpressionContext) clazz.newInstance();
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new JsonParseException("Unable to instantiate " + typeOfT.getTypeName());
            }

            output.expression = output.deserializeExpression(json, output.getElements());

            return output;
        }
    }

    private static class ConstantAdjustmentWrappedExpression implements IExpression {
        final double adjust;

        ConstantAdjustmentWrappedExpression(final double adjust) {
            this.adjust = adjust;
        }

        @Override
        public Double get() {
            return adjust;
        }

        @Override
        public void setIfRequired(final String arg, final Supplier<Double> value) {
            // No-op
        }

        @Override
        public boolean isRequired(final String arg) {
            return false;
        }
    }

    private static class ParameterlessParsedWrappedExpression implements IExpression {
        final Expression expression;

        ParameterlessParsedWrappedExpression(final Expression expression) {
            this.expression = expression;
        }

        @Override
        public Double get() {
            return expression.calculate();
        }

        @Override
        public void setIfRequired(final String arg, final Supplier<Double> value) {
            // No-op
        }

        @Override
        public boolean isRequired(final String arg) {
            return false;
        }
    }

    private static class ParsedWrappedExpression extends ParameterlessParsedWrappedExpression {
        final Map<String, Argument> elements;

        ParsedWrappedExpression(final Expression expression, final Argument[] elements) {
            super(expression);

            // TODO: This doesn't handle argument names that are part of a longer one
            // ie: "current" and "currentOther"
            final String expr = expression.getExpressionString();
            this.elements = Arrays
                .stream(elements)
                .filter((a) -> expr.contains(a.getArgumentName()))
                .collect(
                    Collectors
                        .toMap(Argument::getArgumentName, (a) -> a)
                );
        }

        @Override
        public Double get() {
            return expression.calculate();
        }

        @Override
        public void setIfRequired(final String argName, final Supplier<Double> value) {
            final Argument arg = elements.get(argName);
            if(arg == null) return;

            arg.setArgumentValue(value.get());
        }

        @Override
        public boolean isRequired(final String arg) {
            return elements.containsKey(arg);
        }
    }
}
