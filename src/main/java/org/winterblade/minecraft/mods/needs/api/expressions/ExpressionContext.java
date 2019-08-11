package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.winterblade.minecraft.mods.needs.api.documentation.IDocumentedContext;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonAdapter(ExpressionContext.Deserializer.class)
public abstract class ExpressionContext implements IExpression, IDocumentedContext {
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
    public Double apply(final PlayerEntity player) {
        return expression.apply(player);
    }

    public boolean isRequired(final String arg) {
        return expression.isRequired(arg);
    }

    @Override
    public IExpression setIfRequired(final String arg, final Supplier<Double> value) {
        expression.setIfRequired(arg, value);
        return this;
    }

    @Override
    public List<LazyNeed> getNeeds() {
        return expression.getNeeds();
    }

    public void syncAll() {
        expression.getNeeds().forEach((n) -> n.get(Need::enableSyncing, () -> {}));
    }

    public boolean isConstant() {
        return expression instanceof ConstantAdjustmentWrappedExpression;
    }

    public abstract List<String> getElements();

    private IExpression deserializeExpression(final JsonElement json, final List<String> elements) {
        return deserializeExpression(
            json,
            elements
                .stream()
                .map((a) -> new Argument(a, 0))
                .toArray(Argument[]::new)
        );
    }

    private IExpression deserializeExpression(final JsonElement json, Argument[] elements) {
        if (!json.isJsonPrimitive()) throw new JsonParseException("Expression must be a string or an integer");

        final JsonPrimitive primitive = json.getAsJsonPrimitive();

        if(primitive.isNumber()) {
            return new ConstantAdjustmentWrappedExpression(primitive.getAsDouble());
        }

        // Going to a primitive then a string avoids string builder'ing it all
        String exp = json.getAsJsonPrimitive().getAsString().trim();

        final Map<String, LazyNeed> needs = new HashMap<>();
        final Set<String> matched = new HashSet<>();
        if (exp.contains("need(")) {
            final Matcher match = Pattern.compile("need\\(([a-zA-Z0-9 ]+)\\)").matcher(exp);
            char current = 'A';
            final StringBuilder prefix = new StringBuilder("need");

            while (match.find()) {
                final String name = match.group(1).toLowerCase();
                if (!matched.add(name)) continue; // We've already handled it

                // Do the swap
                final String rep = prefix.toString() + current;
                needs.put(rep, new LazyNeed(name));
                exp = exp.replaceAll("need\\(" + match.group(1) + "\\)", rep); // TODO: Fix edge case based on two separate capitalizations...

                // Increment our stuff
                current++;
                if ('Z' < current) {
                    current = 'A';
                    prefix.append('A'); // I mean, yeah, this makes it 'needAAAAAA' but, pls. needAZ is already 52 needs.
                }
            }

            // Add our fancy new elements in...
            elements = ArrayUtils.addAll(
                        elements,
                        needs
                            .entrySet()
                            .stream()
                            .map((kv) -> new Argument(kv.getKey(), 0))
                            .toArray(Argument[]::new)
            );
        }

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

        if (elements.length <= 0) return new ConstantAdjustmentWrappedExpression(expression.calculate());


        final String expr = expression.getExpressionString();
        final Map<String, Argument> elemMap = Arrays
                .stream(elements)
                .filter((a) -> expr.contains(a.getArgumentName()))
                .collect(
                        Collectors
                                .toMap(Argument::getArgumentName, (a) -> a)
                );

        return elemMap.isEmpty()
            ? new ConstantAdjustmentWrappedExpression(expression.calculate())
            : needs.isEmpty()
                ? new ParsedWrappedExpression(expression, elemMap)
                : new NeedWrappedExpression(expression, elemMap, needs);
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
        public Double apply(final PlayerEntity player) {
            return adjust;
        }

        @Override
        public IExpression setIfRequired(final String arg, final Supplier<Double> value) {
            // No-op
            return this;
        }

        @Override
        public boolean isRequired(final String arg) {
            return false;
        }

        @Override
        public List<LazyNeed> getNeeds() {
            return Collections.emptyList();
        }
    }

    private static class ParsedWrappedExpression implements IExpression {
        final Expression expression;
        final Map<String, Argument> elements;

        ParsedWrappedExpression(final Expression expression, final Map<String, Argument> elements) {
            this.expression = expression;
            this.elements = elements;
        }

        @Override
        public Double apply(final PlayerEntity player) {
            return expression.calculate();
        }

        @Override
        public IExpression setIfRequired(final String argName, final Supplier<Double> value) {
            final Argument arg = elements.get(argName);
            if(arg == null) return this;

            arg.setArgumentValue(value.get());
            return this;
        }

        @Override
        public boolean isRequired(final String arg) {
            return elements.containsKey(arg);
        }

        @Override
        public List<LazyNeed> getNeeds() {
            return Collections.emptyList();
        }
    }

    private static class NeedWrappedExpression extends ParsedWrappedExpression {
        private final Map<String, LazyNeed> needs;

        NeedWrappedExpression(final Expression expression, final Map<String, Argument> elements, final Map<String, LazyNeed> needs) {
            super(expression, elements);
            this.needs = needs;
        }

        @Override
        public Double apply(final PlayerEntity player) {
            for (final Map.Entry<String, LazyNeed> kv : needs.entrySet()) {
                setIfRequired(kv.getKey(), () -> kv.getValue().getValueFor(player));
            }

            return super.apply(player);
        }

        @Override
        public List<LazyNeed> getNeeds() {
            return ImmutableList.copyOf(needs.values());
        }
    }
}
