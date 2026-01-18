package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ChatDebugCommand extends AbstractCommand {

    private static final String PERM_CHATDEBUG = "hytale.command.chatdebug";

    private final LocalGlobalChatPlugin plugin;

    public ChatDebugCommand(LocalGlobalChatPlugin plugin) {
        super("chatdebug", "Toggles chat debug (shows targets)");
        this.plugin = plugin;

        LGChatCompat.requirePermissionNode(this, PERM_CHATDEBUG);

        addAliases("cdg");
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        UUID uuid = context.sender().getUuid();
        if (uuid == null) {
            context.sender().sendMessage(Message.raw("This command can only be used by players."));
            return CompletableFuture.completedFuture(null);
        }

        if (!LGChatCompat.hasPermissionCompat(context.sender(), PERM_CHATDEBUG)) {
            context.sender().sendMessage(Message.raw("You don't have permission to use /chatdebug."));
            return CompletableFuture.completedFuture(null);
        }

        plugin.toggleDebug(uuid);
        boolean now = plugin.isDebug(uuid);

        context.sender().sendMessage(plugin.buildChatDebugMessage(context.sender(), uuid, now));
        return CompletableFuture.completedFuture(null);
    }
}
