package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.IDocumentedContext;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonAdapter(ExpressionContext.Deserializer.class)
public abstract class ExpressionContext implements IExpression, IDocumentedContext {

    // Ensure default constructor?
    protected ExpressionContext() {}

    IExpression expression;

    // Storage for building
    private List<Argument> arguments;
    private Supplier<IExpression> builderFn;
    private Function<PlayerEntity, Double> applyFn = (p) -> {
        build();
        return expression.apply(p);
    };

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

    /**
     * Calculates the function and returns the result
     * @param player The player to calculate on
     * @return The result
     */
    @Override
    public Double apply(final PlayerEntity player) {
        return applyFn.apply(player);
    }

    /**
     * Adds an argument by name
     * @param name The name of the argument
     */
    public void addArgument(final String name) {
        if (expression != null) {
            NeedsMod.LOGGER.warn("Attempted to add argument '" + name + "' after expression was built.");
            return;
        }

        arguments.add(new Argument(name, 0d));
    }

    /**
     * Checks if the argument is required
     * @param arg The argument
     * @return True if so, false otherwise
     */
    public boolean isRequired(final String arg) {
        return expression.isRequired(arg);
    }

    /**
     * Sets an argument if it's necessary, don't otherwise
     * @param arg   The argument
     * @param value A supplier to get the argument, if it's required
     * @return The expression for chaining
     */
    @Override
    public IExpression setIfRequired(final String arg, final Supplier<Double> value) {
        expression.setIfRequired(arg, value);
        return this;
    }

    /**
     * Gets needs associated with this expression
     * @return The list of needs
     */
    @Override
    public List<LazyNeed> getNeeds() {
        return expression.getNeeds();
    }

    /**
     * Syncs any needs this may have
     */
    public void build() {
        if (expression == null && builderFn != null) {
            expression = builderFn.get();
        }

        if (expression == null) throw new IllegalArgumentException("Unable to build expression.");

        applyFn = expression;
        getNeeds().forEach((n) -> n.get(Need::enableSyncing, () -> {})); // Make sure we sync any needs first
    }

    /**
     * Check if the expression is constant (has any variables) or not
     * @return True if it is, false otherwise
     */
    public boolean isConstant() {
        return expression instanceof ConstantAdjustmentWrappedExpression;
    }

    /**
     * Returns a list of element names this object supports by default
     * @return The list of elements
     */
    public abstract List<String> getElements();

    /**
     * Used to deserialize the expression JSON onto the object
     * @param json The JSON to parse
     */
    private void deserializeExpression(final JsonElement json) {
        if (!json.isJsonPrimitive()) throw new JsonParseException("Expression must be a string or an integer");

        final JsonPrimitive primitive = json.getAsJsonPrimitive();

        if(primitive.isNumber()) {
            this.expression = new ConstantAdjustmentWrappedExpression(primitive.getAsDouble());
            this.applyFn = this.expression;
            return;
        }

        arguments = getElements()
                .stream()
                .map((a) -> new Argument(a, 0))
                .collect(Collectors.toList());

        // Going to a primitive then a string avoids string builder'ing it all
        final Map<String, LazyNeed> needs = new HashMap<>();
        final String finalExpr = parseExpressionForNeeds(json.getAsJsonPrimitive().getAsString().trim(), arguments, needs);

        // Instead of a constant, we'll allow the expression to be built lazily
        // so that consumers can add additional elements before parsing
        builderFn = () -> {
            final Expression parsedExpr = new Expression(finalExpr, arguments.toArray(new Argument[0]));

            if (!parsedExpr.checkSyntax()) {
                throw new IllegalArgumentException(
                    "Unable to parse expression '" + finalExpr + "', valid variables in this context are: "
                    + String.join(", ",
                        arguments
                            .stream()
                            .map(Argument::getArgumentName)
                            .collect(Collectors.toList())
                    ) + "\n" + parsedExpr.getErrorMessage()
                );
            }

            if (arguments.size() <= 0) {
                return new ConstantAdjustmentWrappedExpression(parsedExpr.calculate());
            }


            final String expr = parsedExpr.getExpressionString();
            final Map<String, Argument> elemMap = arguments
                    .stream()
                    .filter((a) -> expr.contains(a.getArgumentName()))
                    .collect(
                            Collectors
                                    .toMap(Argument::getArgumentName, (a) -> a)
                    );

            return elemMap.isEmpty()
                    ? new ConstantAdjustmentWrappedExpression(parsedExpr.calculate())
                    : needs.isEmpty()
                        ? new ParsedWrappedExpression(parsedExpr, elemMap)
                        : new NeedWrappedExpression(parsedExpr, elemMap, needs);
        };
    }

    /**
     * Parses the expression string in order to pull out any needs that are in it, updating elements, and needs
     * collections, and returning the updated expression
     * @param exp       The expression to parse and return
     * @param arguments The element list to add to
     * @param needs     The the needs map to add to
     * @return The updated expression string
     */
    private String parseExpressionForNeeds(String exp, final List<Argument> arguments, final Map<String, LazyNeed> needs) {
        if (!exp.contains("need(")) return exp;

        final Set<String> matched = new HashSet<>();
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
        needs
            .entrySet()
            .stream()
            .map((kv) -> new Argument(kv.getKey(), 0))
            .forEach(arguments::add);

        return exp;
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

            output.deserializeExpression(json);

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
