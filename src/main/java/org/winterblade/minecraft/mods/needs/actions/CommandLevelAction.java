package org.winterblade.minecraft.mods.needs.actions;

import com.google.gson.annotations.Expose;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.actions.IReappliedOnDeathLevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import javax.annotation.Nonnull;

@Document(description = "Runs a command when entering or exiting the level, optionally re-running it on respawn.")
public class CommandLevelAction extends LevelAction implements IReappliedOnDeathLevelAction, ICommandSource {
    @Expose
    @Document(description = "The command to run; `@p` will target the player affected")
    private String command;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Should the command be re-run when the player respawns")
    @OptionalField(defaultValue = "False")
    private boolean reapply = false;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Should the command be logged")
    @OptionalField(defaultValue = "False")
    private boolean log = false;

    @Override
    public String getName() {
        return "Command";
    }

    @Override
    public void validate(final Need parentNeed, final NeedLevel parentLevel) throws IllegalArgumentException {
        if (command == null || command.isEmpty()) throw new IllegalArgumentException("Must specify a command to run.");
        super.validate(parentNeed, parentLevel);
    }

    @Override
    public void onLoaded(final Need parentNeed, final NeedLevel parentLevel) {
        super.onLoaded(parentNeed, parentLevel);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
    }

    @Override
    public void onEntered(final Need need, final NeedLevel level, final PlayerEntity player) {
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server == null) {
            NeedsMod.LOGGER.warn("Unable to find command manager to run level command: " + command);
            return;
        }

        final Commands cm = server.getCommandManager();
        cm.handleCommand(new CommandSource(
                this,
                player.getPositionVector(),
                player.getPitchYaw(),
                (ServerWorld)player.world,
                4,
                need.getName(),
                new StringTextComponent(need.getName()),
                server,
                player
            ),
            command
        );
    }

    @Override
    public void onExited(final Need need, final NeedLevel level, final PlayerEntity player) {
        onEntered(need, level, player);
    }

    @Override
    public void onContinuousStart(final Need need, final NeedLevel level, final PlayerEntity player) {
        onEntered(need, level, player);
    }


    @Override
    public void onRespawned(final Need need, final NeedLevel level, final PlayerEntity player, final PlayerEntity oldPlayer) {
        if (!reapply) return;
        onEntered(need, level, player);
    }

    @Override
    public void onRespawnedWhenContinuous(final Need need, final NeedLevel level, final PlayerEntity player, final PlayerEntity oldPlayer) {
        if (!reapply) return;
        onEntered(need, level, player);
    }

    @Override
    public void sendMessage(@Nonnull final ITextComponent component) {
        NeedsMod.LOGGER.info(component.getString());
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    @Override
    public boolean shouldReceiveErrors() {
        return true;
    }

    @Override
    public boolean allowLogging() {
        return log;
    }
}
