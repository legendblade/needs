package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
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
    @OptionalField(defaultValue = "0")
    @Document(description = "Sets the number of decimal places displayed in the UI; by default, this is 0")
    @SuppressWarnings("FieldMayBeFinal")
    private int precision = 0;

    @Expose
    @JsonAdapter(ColorSet.Deserializer.class)
    @Document(description = "Sets the RGB color of the bar; this can either be the integer representation of the color, " +
            "or you can specify a hexidecimal string starting with either # or 0x - when specifying it as hex, you " +
            "may use shorthand (e.g. `'#777'` will expand to `'#777777'`).")
    private ColorSet color;


    @Expose
    @Document(description = "Optional expression that will be used to modify the value prior to displaying it (in case you " +
            "want to multiply it, round it, square root things, etc). The min, max, value, and upper and lower bounds of the " +
            "current level will all be run through this expression.")
    @OptionalField(defaultValue = "None")
    protected NeedExpressionContext displayFormat;

    private boolean shouldDisplay = true;
    private String precisionFormat;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        icon.validate();
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        need.enableSyncing();

        precisionFormat = "%." + precision + "f";
        if (displayFormat != null) displayFormat.build();
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

    public String getPrecision() {
        return precisionFormat;
    }

    public double doFormat(final double input, final PlayerEntity player) {
        if (displayFormat == null) return input;

        displayFormat.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> input);
        return displayFormat.apply(player);
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