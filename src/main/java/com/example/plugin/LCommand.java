package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LCommand extends AbstractCommand {

    private final LocalGlobalChatPlugin plugin;

    public LCommand(LocalGlobalChatPlugin plugin) {
        super("l", "Alterna para o chat LOCAL (raio configuravel)");
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

        plugin.setMode(uuid, ChatMode.LOCAL);
        context.sender().sendMessage(Message.raw("Agora voce esta no chat LOCAL (" + plugin.getLocalRadiusInt() + " blocos). [L]"));
        return CompletableFuture.completedFuture(null);
    }
}