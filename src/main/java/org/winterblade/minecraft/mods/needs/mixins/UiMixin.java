package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.gui.ExpressionPositionedTexture;
import org.winterblade.minecraft.mods.needs.util.ColorAdapter;
import org.winterblade.minecraft.mods.needs.util.DynamicTextureUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@Document(description = "Adds the need to a UI display so players can easily track their current level")
public class UiMixin extends BaseMixin {
    public static final UiMixin NONE;
    public static final ExpressionPositionedTexture GENERIC_ICON =
            new ExpressionPositionedTexture(new ResourceLocation(NeedsMod.MODID, "gui/generic_need.png"), 34, 34);
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
            "with just the icon name (e.g. 'w_sword001'), a texture from another mod by using the full path " +
            "(e.g. 'minecraft:textures/item/iron_helmet'), or a local image placed in the 'config/needs/textures' " +
            "directory (e.g. 'file:path/to/image.png')")
    @SuppressWarnings("FieldMayBeFinal")
    private String icon;

    @Expose
    @OptionalField(defaultValue = "32")
    @Document(description = "The width of the icon; this defaults to 32px")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconWidth = 32;

    @Expose
    @OptionalField(defaultValue = "32")
    @Document(description = "The height of the icon; this defaults to 32px")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconHeight = 32;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the X coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private NeedExpressionContext iconX = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the Y coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private NeedExpressionContext iconY = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    @Expose
    @OptionalField(defaultValue = "(32-iconWidth)/2")
    @Document(description = "Adjust this if you want to adjust the X offset of the icon in the UI; by default, it " +
            "will be (32-iconWidth)/2")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconOffsetX = Integer.MIN_VALUE;

    @Expose
    @OptionalField(defaultValue = "(32-iconHeight)/2")
    @Document(description = "Adjust this if you want to adjust the Y offset of the icon in the UI; by default, it " +
            "will be (32-iconHeight)/2")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconOffsetY = Integer.MIN_VALUE;

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
    @OptionalField(defaultValue = "Icon Width")
    @Document(description = "The width of the icon texture; if using a texture sheet, this should be the width of the sheet")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconTextureWidth = Integer.MIN_VALUE;

    @Expose
    @OptionalField(defaultValue = "Icon Height")
    @Document(description = "The height of the icon texture; if using a texture sheet, this should be the height of the sheet")
    @SuppressWarnings("FieldMayBeFinal")
    private int iconTextureHeight = Integer.MIN_VALUE;

    @Expose
    @Document(description = "Optional expression that will be used to modify the value prior to displaying it (in case you " +
            "want to multiply it, round it, square root things, etc). The min, max, value, and upper and lower bounds of the " +
            "current level will all be run through this expression.")
    @OptionalField(defaultValue = "None")
    protected NeedExpressionContext displayFormat;

    private boolean shouldDisplay = true;
    private ExpressionPositionedTexture iconTexture;
    private String precisionFormat;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (iconX == null) throw new IllegalArgumentException("iconX cannot be null.");
        if (iconY == null) throw new IllegalArgumentException("iconY cannot be null.");
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        need.enableSyncing();

        if (iconOffsetX == Integer.MIN_VALUE) iconOffsetX = (32 - iconWidth) / 2;
        if (iconOffsetY == Integer.MIN_VALUE) iconOffsetY = (32 - iconHeight) / 2;
        if (iconTextureWidth == Integer.MIN_VALUE) iconTextureWidth = iconWidth;
        if (iconTextureHeight == Integer.MIN_VALUE) iconTextureHeight = iconHeight;

        if (icon != null && icon.startsWith("file:")) {
            iconTexture = GENERIC_ICON;
            final Supplier<ResourceLocation> dynamicLoc = DynamicTextureUtil.getDynamicTexture(icon);

            if (dynamicLoc != null) {
                iconTexture = new ExpressionPositionedTexture(
                        dynamicLoc,
                        iconTextureWidth,
                        iconTextureHeight,
                        iconX,
                        iconY,
                        iconWidth,
                        iconHeight
                );
            }
        } else if (icon != null && !icon.isEmpty()) {
            if (!icon.contains(".")) icon += ".png";
            final String[] split = icon.split(":");

            if (split.length <= 0) iconTexture = GENERIC_ICON;
            else if (split.length <= 1) {
                iconTexture = new ExpressionPositionedTexture(new ResourceLocation(NeedsMod.MODID, "textures/gui/needs/" + icon),32,32);
            } else {
                iconTexture = new ExpressionPositionedTexture(
                    new ResourceLocation(split[0], split[1]),
                    iconTextureWidth,
                    iconTextureHeight,
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

        // If we have any dependent needs, make sure they're synced.
        iconX.syncAll();
        iconY.syncAll();
        if (displayFormat != null) displayFormat.syncAll();
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
        return displayFormat.apply(DistExecutor.runForDist(() -> () -> Minecraft.getInstance().player, () -> () -> null));
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