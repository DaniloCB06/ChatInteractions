package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class LocalRadiusCommand extends AbstractCommand {

    private static final String PERM_LOCALRADIUS = "hytale.command.localradius";

    private final LocalGlobalChatPlugin plugin;
    private final RequiredArg<String> blocksArg;

    public LocalRadiusCommand(LocalGlobalChatPlugin plugin) {
        super("localradius", "Changes the local chat radius (in blocks)");
        this.plugin = plugin;

        LGChatCompat.requirePermissionNode(this, PERM_LOCALRADIUS);

        // Use STRING to be compatible with any build (then convert to int)
        blocksArg = withRequiredArg("blocks", "Local radius size in blocks", ArgTypes.STRING);
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!LGChatCompat.hasPermissionCompat(context.sender(), PERM_LOCALRADIUS)) {
            context.sender().sendMessage(Message.raw("You don't have permission to use /localradius."));
            return CompletableFuture.completedFuture(null);
        }

        String raw = context.get(blocksArg);
        if (raw == null || raw.trim().isEmpty()) {
            context.sender().sendMessage(Message.raw("Usage: /localradius <number of blocks>"));
            return CompletableFuture.completedFuture(null);
        }

        int blocks;
        try {
            blocks = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            context.sender().sendMessage(Message.raw("Invalid value. Use an integer number. Example: /localradius 80"));
            return CompletableFuture.completedFuture(null);
        }

        plugin.setLocalRadius(blocks);
        context.sender().sendMessage(Message.raw("LOCAL chat radius is now " + plugin.getLocalRadiusInt() + " blocks."));
        return CompletableFuture.completedFuture(null);
    }
}
