package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.Formatting;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.gui.ExpressionPositionedTexture;
import org.winterblade.minecraft.mods.needs.util.ColorSet;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Document(description = "Adds the need to a UI display so players can easily track their current level")
public class UiMixin extends BaseMixin {
    public static final UiMixin NONE;
    private static List<LocalCachedNeed> sortedLocalCache;

    static {
        NONE = new UiMixin();
        NONE.shouldDisplay = false;
        MinecraftForge.EVENT_BUS.addListener(UiMixin::onCacheUpdated);
    }

    @Expose
    @OptionalField(defaultValue = "The need's name")
    @Document(description = "The name to display in the UI; this can be different from the need itself")
    @SuppressWarnings("FieldMayBeFinal")
    private String displayName;

    @Expose
    @Document(description = "The icon to use to represent the need")
    private Icon icon;

    @Expose
    @JsonAdapter(ColorSet.Deserializer.class)
    @Document(description = "Sets the RGB color of the bar; this can either be the integer representation of the color, " +
            "or you can specify a hexidecimal string starting with either # or 0x - when specifying it as hex, you " +
            "may use shorthand (e.g. `'#777'` will expand to `'#777777'`).")
    private ColorSet color;


    @Expose
    @Document(description = "Defines any extra formatting parameters to apply")
    @OptionalField(defaultValue = "None")
    protected Formatting formatting;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The minimum value to display in the UI; the actual displayed minimum will be either this " +
            "or the minimum value of the need, whichever is greater.")
    @OptionalField(defaultValue = "None")
    private double min = Double.NEGATIVE_INFINITY;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The maximum value to display in the UI; the actual displayed maximum will be either this " +
            "or the maximum value of the need, whichever is lesser.")
    @OptionalField(defaultValue = "None")
    private double max = Double.POSITIVE_INFINITY;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The minimum value of the bar in the UI; the actual displayed minimum will be either this, " +
            "the value of min, or the minimum value of the need, whichever is the greatest.")
    @OptionalField(defaultValue = "None")
    private double barMin = Double.NEGATIVE_INFINITY;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The maximum value of the bar in the UI; the actual displayed maximum will be either this, " +
            "the value of max, or the maximum value of the need, whichever is the least.")
    @OptionalField(defaultValue = "None")
    private double barMax = Double.POSITIVE_INFINITY;

    private boolean shouldDisplay = true;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        icon.validate();
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        need.enableSyncing();

        if (formatting == null) formatting = new Formatting();
        formatting.init();
        icon.onLoaded();
    }

    public boolean shouldDisplay() {
        return shouldDisplay;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName != null && !displayName.isEmpty() ? displayName : need != null ? need.getName() : "Loading...";
    }

    @Nonnull
    public ExpressionPositionedTexture getIconTexture() {
        return icon.getTexture();
    }

    public int getIconOffsetX() {
        return icon.getX();
    }

    public int getIconOffsetY() {
        return icon.getY();
    }

    public int getColor() {
        return getColorVs(0x8b8b8b);
    }

    public int getColorVs(final int background) {
        return color.getAgainst(background);
    }

    public Formatting getFormatting() {
        return formatting;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getBarMin() {
        return barMin;
    }

    public double getBarMax() {
        return barMax;
    }

    /**
     * Gets the mixin from the need, or UiMixin.NONE if there isn't one
     * @param need The need
     * @return The mixin
     */
    @Nonnull
    public static UiMixin getInstance(final Need need) {
        return (UiMixin) need.getMixins().stream().filter((m) -> (m instanceof UiMixin)).findFirst().orElse(NONE);
    }

    /**
     * Called on the client side to get the local cache
     * @return  The cache
     */
    @Nonnull
    public static List<LocalCachedNeed> getLocalNeeds() {
        if (sortedLocalCache != null) return sortedLocalCache;

        // Iterate the map, pulling out any now invalid needs
        sortedLocalCache = new ArrayList<>();

        // This will happen if a config update pulls out a need (which isn't possible yet, but Soon TM)
        NeedRegistry.INSTANCE.getLocalCache().forEach((key, value) -> {
            final Need need = value.getNeed().get();
            if (need == null) return;

            final UiMixin mixin = getInstance(need);
            if (!mixin.shouldDisplay()) return;

            sortedLocalCache.add(value);
        });

        // Finally, sort it by name
        sortedLocalCache.sort(Comparator.comparing(LocalCachedNeed::getName));
        return sortedLocalCache;
    }

    @SuppressWarnings("unused")
    private static void onCacheUpdated(final LocalCacheUpdatedEvent event) {
        // Invalidate local cache
        sortedLocalCache = null;
    }
}