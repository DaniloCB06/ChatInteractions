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
        super("g", "Alterna para o chat GLOBAL");
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
            context.sender().sendMessage(Message.raw("Este comando so pode ser usado por jogadores."));
            return CompletableFuture.completedFuture(null);
        }

        plugin.setMode(uuid, ChatMode.GLOBAL);
        context.sender().sendMessage(Message.raw("Agora voce esta no chat GLOBAL. [G]"));
        return CompletableFuture.completedFuture(null);
    }
}