package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GCommand extends AbstractCommand {

    private final LocalGlobalChatPlugin plugin;

    public GCommand(LocalGlobalChatPlugin plugin) {
        super("g", "Switches to GLOBAL chat");
        this.plugin = plugin;

        LGChatCompat.relaxCommandPermissions(this);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        UUID uuid = context.sender().getUuid();
        if (uuid == null) {
            context.sender().sendMessage(Message.raw("This command can only be used by players."));
            return CompletableFuture.completedFuture(null);
        }

        plugin.setMode(uuid, ChatMode.GLOBAL);
        context.sender().sendMessage(Message.raw("You are now in GLOBAL chat. [G]"));
        return CompletableFuture.completedFuture(null);
    }
}
