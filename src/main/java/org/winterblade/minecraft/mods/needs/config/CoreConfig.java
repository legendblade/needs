package org.winterblade.minecraft.mods.needs.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.client.config.ColorblindSetting;

import java.util.Arrays;
import java.util.stream.Collectors;

import static net.minecraftforge.common.ForgeConfigSpec.*;

@SuppressWarnings("WeakerAccess")
public class CoreConfig {
    public static Client CLIENT;
    public static Server SERVER;

    public static final ForgeConfigSpec clientSpec;
    public static final ForgeConfigSpec serverSpec;

    static {
        final Pair<Server, ForgeConfigSpec> serverPair = new Builder().configure(Server::new);
        serverSpec = serverPair.getRight();
        SERVER = serverPair.getLeft();

        final Pair<Client, ForgeConfigSpec> clientPair = new Builder().configure(Client::new);
        clientSpec = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    public static class Server {
        public final IntValue tickBuckets;

        public Server(final Builder builder) {
            builder.comment("Server configuration settings")
                    .push("server");

            tickBuckets = builder
                    .comment(
                        "Define the number of buckets to create to hold players; this correlates to the number of " +
                        "ticks between individual updates per player except for the tick manipulator which will " +
                        "automatically be multiplied by this value."
                    )
                    .translation(NeedsMod.MODID + ".configgui.tickBuckets")
                    .worldRestart() // Assuming this means game restart? Maybe? IDFK. What's documentation?
                    .defineInRange("tickBuckets", 5, 1, 100);

            builder.pop();
        }
    }

    public static class Client {
        public final EnumValue<ColorblindSetting> colorblindess;

        public Client(final Builder builder) {
            builder.comment("Client configuration settings")
                    .push("client");

            colorblindess = builder
                    .comment(
                        "Enable colorblind specific colorations. Colors must generally be defined by the packmaker, " +
                        "but will default as best as possible otherwise. Valid options: [" +
                        Arrays.stream(ColorblindSetting.values()).map(Enum::name).collect(Collectors.joining(", ")) +
                        "]"
                    )
                    .translation(NeedsMod.MODID + ".configgui.colorblindness")
                    .defineEnum("colorblindness", ColorblindSetting.NORMAL);

            builder.pop();
        }
    }
}
