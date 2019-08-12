package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.OtherNeedChangedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.*;

@SuppressWarnings("WeakerAccess")
@Document(description = "Affect a series of needs in lockstep with one another; this adjusts all linked needs as a single " +
        "atomic action in order to avoid loops. Full chains can and should be defined here, and should only be defined " +
        "in one location. A link will not be updated if updating it would cause an update to an already updated need.")
public class LinkedNeedManipulator extends BaseManipulator {
    @Expose
    @Document(description = "The chain of linked needs to handle; chains will be evaluated starting from the affected need " +
            "and will propagate through until no more needs need to be updated.", type = Link.class)
    protected List<Link> chain = Collections.emptyList();

    @Expose
    @Document(description = "Changes the functionality to only ever visit a linked need once ever when following every " +
            "chain.\n\nBy default, if Need A, B, and C were all linked together, a chain could go from A -> B -> C, and then " +
            "also from A -> C. When this is set to true, the second chain wouldn't happen as it would detect C had been " +
            "visited once already.\n\nIn all cases, the first chain (A -> B -> C) could never go back to A or B, as they " +
            "have been visited in that specific chain.")
    @OptionalField(defaultValue = "False")
    protected boolean onceEver = false;

    protected Multimap<Need, LinkHalf> unravels = ArrayListMultimap.create();
    protected Set<IManipulator> tangles = new HashSet<>();

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (chain.isEmpty()) throw new IllegalArgumentException("Linked needs must have at least one link in the chain.");
        chain.forEach((l) -> {
            if(!Objects.equals(l.left.need, l.right.need)) return;
            throw new IllegalArgumentException("The two sides of a chain cannot be the same: "
                    + l.left.need.toString() + " <-> " + l.right.need.toString());
        });
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        // Build up a list of needs we're concerned about:
        chain.forEach((l) -> {
            if (l.left.need == null) l.left.need = LazyNeed.of(parent);
            if (l.right.need == null) l.right.need = LazyNeed.of(parent);

            l.left.need.get((n) -> prepLink(n, l.left, l.right), () -> NeedsMod.LOGGER.warn("Unable to get chain link " + l.left.need));
            l.right.need.get((n) -> prepLink(n, l.right, l.left), () -> NeedsMod.LOGGER.warn("Unable to get chain link " + l.right.need));
            if (l.left.instance == null || l.right.instance == null) throw new IllegalArgumentException("Unable to complete chain. See previous errors.");
        });
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        chain.forEach((l) -> {
            l.left.need.discard();
            l.right.need.discard();

            l.left.instance = null;
            l.right.instance = null;
        });
        unravels.clear();
    }

    @SubscribeEvent
    public void onNeedChanged(final NeedAdjustmentEvent.Post event) {
        if (tangles.contains(event.getSource())) return;

        final Collection<LinkHalf> halves = unravels.get(event.getNeed());
        if (halves == null || halves.isEmpty()) return;

        // Create a manipulator to block updates from this chain from cascading back into this chain
        final ChainedManipulator tangle = new ChainedManipulator();
        tangles.add(tangle);

        // Create our initial set and chain recorder...
        final Set<Need> visited = new HashSet<>();
        visited.add(event.getNeed());

        final StringBuilder chainOfMemories = new StringBuilder().append("Need chain from ").append(parent.getName()).append(": ").append(event.getNeed().getName());

        // Into the depths
        linker(event.getNeed(), event.getCurrent(), event.getPrevious(), event.getPlayer(), halves, visited, chainOfMemories, tangle);
        tangles.remove(tangle);
    }

    /**
     * Prepares the links to run
     * @param need      The side of the link we're dealing with
     * @param ourSide   Our side of the link
     * @param otherSide The other side of the link
     */
    private void prepLink(final Need need, final LinkHalf ourSide, final LinkHalf otherSide) {
        ourSide.instance = need;
        unravels.put(need, otherSide);
    }

    /**
     * Deals with the recursion of the recursion of the recursion of linking the whole chain together
     * @param from     The need we're chaining from
     * @param current  The current value of that need
     * @param previous The previous value of that need
     * @param player   The player we're working on
     * @param halves   The links we're going through
     * @param visited  The list of visited nodes down this path
     * @param sb       The {@link StringBuilder} we're using to keep track of all this
     * @param tangle   The manipulator we're passing in right now.
     */
    private void linker(final Need from, final double current, final double previous, final PlayerEntity player,
                        final Collection<LinkHalf> halves, final Set<Need> visited, final StringBuilder sb, final ChainedManipulator tangle) {
        // Figure out if we should continue:
        final double diff = current - previous;
        if (diff == 0) {
            NeedsMod.LOGGER.info(sb.append(" => d0").toString());
            return;
        }

        for (final LinkHalf lh : halves) {
            // Check our min/max values, and if we've visited here on this chain and bail otherwise...
            if (current < lh.minValue || lh.maxValue < current || diff < lh.minChange || lh.maxChange < diff || !visited.add(lh.instance)) {
                NeedsMod.LOGGER.info(sb.toString());
                continue; // Drop the chain here if we've already visited it
            }

            // Record our progress
            final StringBuilder sb2 = new StringBuilder(sb);
            sb2.append(" -> ").append(lh.instance.getName());

            // Set it all up...
            final double p2 = lh.instance.getValue(player);
            lh.amount
                    .setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> p2)
                    .setIfRequired(OtherNeedChangedExpressionContext.CHANGE, () -> diff)
                    .setIfRequired(OtherNeedChangedExpressionContext.OTHER, () -> current)
                    .setIfRequired(OtherNeedChangedExpressionContext.PREVIOUS, () -> previous);

            // Do the adjustment and continue the chain
            final double c2 = lh.instance.adjustValue(player, lh.amount.apply(player), tangle);
            linker(lh.instance, c2, p2, player, unravels.get(lh.instance), onceEver ? visited : new HashSet<>(visited), sb2, tangle);
        }
    }

    @Document(description = "An individual link in the chain. All links are symmetrical; e.g. if you adjust the need " +
            "on the left side of the link, the right side will be adjusted by the amount, and if you adjust the right " +
            "side of the link, the left side will be adjusted.")
    protected static class Link {
        @Expose
        @Document(description = "The left side of the link")
        protected LinkHalf left;

        @Expose
        @Document(description = "The right side of the link")
        protected LinkHalf right;
    }

    @Document(description = "Defines one side of the link")
    protected static class LinkHalf {
        @Expose
        @OptionalField(defaultValue = "The parent need")
        @Document(description = "The need for this side of the link")
        protected LazyNeed need;

        @Expose
        @OptionalField(defaultValue = "None")
        @Document(description = "The minimum amount the other need must have changed by in order to trigger this manipulator")
        protected double minChange = Double.NEGATIVE_INFINITY;

        @Expose
        @OptionalField(defaultValue = "None")
        @Document(description = "The maximum amount the other need must have changed by in order to trigger this manipulator")
        protected double maxChange = Double.POSITIVE_INFINITY;

        @Expose
        @OptionalField(defaultValue = "None")
        @Document(description = "The minimum current amount of the other need in order to trigger this manipulator")
        protected double minValue = Double.NEGATIVE_INFINITY;

        @Expose
        @OptionalField(defaultValue = "None")
        @Document(description = "The maximum current amount of the other need in order to trigger this manipulator")
        protected double maxValue = Double.POSITIVE_INFINITY;

        @Expose
        @Document(description = "The amount to change by when triggered")
        protected ChainExpressionContext amount;

        protected Need instance;
    }

    protected static class ChainedManipulator extends BaseManipulator {}

    @JsonAdapter(ExpressionContext.Deserializer.class)
    protected static class ChainExpressionContext extends OtherNeedChangedExpressionContext {
        protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);
        static {
            docs.put(CURRENT_NEED_VALUE, "The current value of this side of the chain.");
            docs.put(OTHER, "The current value of the other side of the chain.");
            docs.put(PREVIOUS, "The previous value of the other side of the chain.");
            docs.put(CHANGE, "The amount the other side of the chain changed by, or: (current - previous).");
        }

        public ChainExpressionContext() {
        }

        @Override
        public Map<String, String> getElementDocumentation() {
            return docs;
        }
    }
}
