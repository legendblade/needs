package org.winterblade.minecraft.mods.needs.util.expressions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.function.Function;

@JsonAdapter(Expression.Deserializer.class)
public abstract class Expression {
    public abstract int calculate(int input);

    public static class Deserializer implements JsonDeserializer<Expression> {

        @Override
        public Expression deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonPrimitive()) throw new JsonParseException("Expression must be a string or an integer");

            JsonPrimitive primitive = json.getAsJsonPrimitive();

            if(primitive.isNumber()) {
                return new ConstantAdjustmentExpression(primitive.getAsInt());
            }

            // Going to a primitive then a string avoids string builder'ing it all
            String exp = json.getAsJsonPrimitive().getAsString().trim();
            if (!exp.contains("x")) {
                if (exp.startsWith("+")) return new ConstantAdjustmentExpression(Integer.parseInt(exp.substring(1)));
                if (exp.startsWith("-")) return new ConstantAdjustmentExpression(0-Integer.parseInt(exp.substring(1)));
                if (exp.startsWith("*")) return new ConstantMultiplicationExpression(Integer.parseInt(exp.substring(1)));
                if (exp.startsWith("/")) return new ConstantDivisionExpression(Integer.parseInt(exp.substring(1)));
                if (exp.startsWith("=")) return new ConstantExpression(Integer.parseInt(exp.substring(1)));
                return new ConstantAdjustmentExpression(Integer.parseInt(exp));
            }

            // The easy ones:
            if(exp.equals("=x")) return new FunctionExpression((i) -> i);
            if(exp.equals("*x")) return new FunctionExpression((i) -> i * i);
            if(exp.equals("x") || exp.equals("+x")) return new FunctionExpression((i) -> i * 2);

            // The ones that make no dang sense:
            if(exp.equals("-x")) return new ConstantExpression(0);
            if(exp.equals("/x")) return new ConstantExpression(1);

            throw new JsonParseException("Complex expressions like '" + exp + "' are currently not supported.");
        }
    }

    private static class FunctionExpression extends Expression {
        private final Function<Integer, Integer> expr;

        FunctionExpression(Function<Integer, Integer> expr) {
            this.expr = expr;
        }

        @Override
        public int calculate(int input) {
            return expr.apply(input);
        }
    }

    private static class ConstantAdjustmentExpression extends Expression {
        final int adjust;

        ConstantAdjustmentExpression(int adjust) {
            super();
            this.adjust = adjust;
        }

        @Override
        public int calculate(int input) {
            return input + adjust;
        }
    }

    private static class ConstantMultiplicationExpression extends ConstantAdjustmentExpression {

        ConstantMultiplicationExpression(int adjust) {
            super(adjust);
        }

        @Override
        public int calculate(int input) {
            return input * adjust;
        }
    }

    private static class ConstantDivisionExpression extends ConstantAdjustmentExpression {

        ConstantDivisionExpression(int adjust) {
            super(adjust);
        }

        @Override
        public int calculate(int input) {
            return input / adjust;
        }
    }

    private static class ConstantExpression extends Expression {
        private final int value;

        ConstantExpression(int value) {
            this.value = value;
        }

        @Override
        public int calculate(int input) {
            return value;
        }
    }
}
