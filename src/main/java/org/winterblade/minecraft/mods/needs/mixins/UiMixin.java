package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.util.ColorAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("FieldMayBeFinal")
    private String displayName;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private String icon;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int iconWidth = 32;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int iconHeight = 32;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int iconX = 0;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int iconY = 0;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int iconOffsetX = 0;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int iconOffsetY = 0;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    private int precision = 0;

    @Expose
    @JsonAdapter(ColorAdapter.class)
    private int color;

    private boolean shouldDisplay = true;
    private Texture iconTexture;
    private String precisionFormat;

    public UiMixin() {
        iconOffsetX = (32 - iconWidth) / 2;
        iconOffsetY = (32 - iconHeight) / 2;
    }

    @Override
    public void onCreated(final Need need) {
        super.onCreated(need);
        need.enableSyncing();

        if (icon != null && !icon.isEmpty()) {
            if (!icon.contains("\\.")) icon += ".png";
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