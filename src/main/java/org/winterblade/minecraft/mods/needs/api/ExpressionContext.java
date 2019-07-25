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

    private IExpression expression;

    @Override
    public Double get() {
        return expression.get();
    }

    @Override
    public void setIfRequired(String arg, Supplier<Double> value) {
        expression.setIfRequired(arg, value);
    }

    protected abstract List<String> getElements();

    private IExpression deserializeExpression(JsonElement json, List<String> elements) {
        return deserializeExpression(
            json,
            elements
                .stream()
                .map((a) -> new Argument(a, 0))
                .toArray(Argument[]::new)
        );
    }

    private IExpression deserializeExpression(JsonElement json, Argument[] elements) {
        if (!json.isJsonPrimitive()) throw new JsonParseException("Expression must be a string or an integer");

        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if(primitive.isNumber()) {
            return new ConstantAdjustmentWrappedExpression(primitive.getAsDouble());
        }

        // Going to a primitive then a string avoids string builder'ing it all
        String exp = json.getAsJsonPrimitive().getAsString().trim();
        Expression expression = new Expression(exp, elements);

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
        public ExpressionContext deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ExpressionContext output;
            Class<?> clazz = TypeToken.of(typeOfT).getRawType();

            try {
                output = (ExpressionContext) clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new JsonParseException("Unable to instantiate " + typeOfT.getTypeName());
            }

            output.expression = output.deserializeExpression(json, output.getElements());

            return output;
        }
    }

    private static class ConstantAdjustmentWrappedExpression implements IExpression {
        final double adjust;

        ConstantAdjustmentWrappedExpression(double adjust) {
            this.adjust = adjust;
        }

        @Override
        public Double get() {
            return adjust;
        }

        @Override
        public void setIfRequired(String arg, Supplier<Double> value) {
            // No-op
        }
    }

    private static class ParameterlessParsedWrappedExpression implements IExpression {
        final Expression expression;

        ParameterlessParsedWrappedExpression(Expression expression) {
            this.expression = expression;
        }

        @Override
        public Double get() {
            return expression.calculate();
        }

        @Override
        public void setIfRequired(String arg, Supplier<Double> value) {
            // No-op
        }
    }

    private static class ParsedWrappedExpression extends ParameterlessParsedWrappedExpression {
        final Map<String, Argument> elements;

        ParsedWrappedExpression(Expression expression, Argument[] elements) {
            super(expression);

            // TODO: This doesn't handle argument names that are part of a longer one
            // ie: "current" and "currentOther"
            String expr = expression.getExpressionString();
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
        public void setIfRequired(String argName, Supplier<Double> value) {
            Argument arg = elements.get(argName);
            if(arg == null) return;

            arg.setArgumentValue(value.get());
        }
    }
}
