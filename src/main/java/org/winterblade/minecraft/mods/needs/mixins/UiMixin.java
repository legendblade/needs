package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.util.ColorAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Document(description = "Adds the need to a UI display so players can easily track their current level")
public class UiMixin extends BaseMixin {
    public static final UiMixin NONE;
    public static final Texture GENERIC_ICON = new Texture(new ResourceLocation(NeedsMod.MODID, "gui/generic_need.png"), 34, 34);
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
    @Document(description = "A path to an icon to use; Needs comes with a large preset amount, which can be specified " +
            "with just the icon name (e.g. 'w_sword001'), or a texture from another mod by using the full path " +
            "(e.g. 'minecraft:textures/item/iron_helmet')")
    @SuppressWarnings("FieldMayBeFinal")
    private String icon;

    @Expose
    @OptionalField(defaultValue = "32")
    @Document(description = "The width of the icon texture; this defaults to 32px")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconWidth = 32;

    @Expose
    @OptionalField(defaultValue = "32")
    @Document(description = "The height of the icon texture; this defaults to 32px")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconHeight = 32;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the X coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconX = 0;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the Y coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconY = 0;

    @Expose
    @OptionalField(defaultValue = "(32-iconWidth)/2")
    @Document(description = "Adjust this if you want to adjust the X offset of the icon in the UI; by default, it " +
            "will be (32-iconWidth)/2")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconOffsetX = 0;

    @Expose
    @OptionalField(defaultValue = "(32-iconHeight)/2")
    @Document(description = "Adjust this if you want to adjust the Y offset of the icon in the UI; by default, it " +
            "will be (32-iconHeight)/2")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconOffsetY = 0;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "Sets the number of decimal places displayed in the UI; by default, this is 0")
    @SuppressWarnings("FieldMayBeFinal")
    private int precision = 0;

    @Expose
    @JsonAdapter(ColorAdapter.class)
    @Document(description = "Sets the RGB color of the bar; this can either be the integer representation of the color, " +
            "or you can specify a hexidecimal string starting with either # or 0x - when specifying it as hex, you " +
            "may use shorthand (e.g. '#777' will expand to '#777777').")
    private int color;

    @Expose
    @Document(description = "Optional expression that will be used to modify the value prior to displaying it (in case you " +
            "want to multiply it, round it, square root things, etc). The min, max, value, and upper and lower bounds of the " +
            "current level will all be run through this expression.")
    @OptionalField(defaultValue = "None")
    protected NeedExpressionContext displayFormat;

    private boolean shouldDisplay = true;
    private Texture iconTexture;
    private String precisionFormat;

    public UiMixin() {
        iconOffsetX = (32 - iconWidth) / 2;
        iconOffsetY = (32 - iconHeight) / 2;
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        need.enableSyncing();

        if (icon != null && !icon.isEmpty()) {
            if (!icon.contains(".")) icon += ".png";
            final String[] split = icon.split(":");

            if (split.length <= 0) iconTexture = GENERIC_ICON;
            else if (split.length <= 1) {
                iconTexture = new Texture(new ResourceLocation(NeedsMod.MODID, "textures/gui/needs/" + icon),32,32);
            } else {
                iconTexture = new Texture(
                    new ResourceLocation(split[0], split[1]),
                    iconWidth,
                    iconHeight,
                    iconX,
                    iconY,
                    iconWidth,
                    iconHeight
                );
            }
        } else {
            iconTexture = GENERIC_ICON;
        }
        precisionFormat = "%." + precision + "f";
    }

    public boolean shouldDisplay() {
        return shouldDisplay;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName == null || displayName.isEmpty() ? need.getName() : displayName;
    }

    @Nonnull
    public Texture getIconTexture() {
        return iconTexture != null ? iconTexture : GENERIC_ICON;
    }

    public int getIconOffsetX() {
        return iconOffsetX;
    }

    public int getIconOffsetY() {
        return iconOffsetY;
    }

    public int getColor() {
        return color;
    }

    public String getPrecision() {
        return precisionFormat;
    }

    public double doFormat(final double input) {
        if (displayFormat == null) return input;

        displayFormat.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> input);
        return displayFormat.get();
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
            final Need need = NeedRegistry.INSTANCE.getByName(key);
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