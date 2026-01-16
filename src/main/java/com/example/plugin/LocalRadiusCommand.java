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
        super("localradius", "Altera o raio do chat local (em blocos)");
        this.plugin = plugin;

        LGChatCompat.requirePermissionNode(this, PERM_LOCALRADIUS);

        // usa STRING pra ser compatível com qualquer build (e converte para int)
        blocksArg = withRequiredArg("blocos", "Quantidade de blocos do raio local", ArgTypes.STRING);
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!LGChatCompat.hasPermissionCompat(context.sender(), PERM_LOCALRADIUS)) {
            context.sender().sendMessage(Message.raw("Voce nao tem permissao para usar /localradius."));
            return CompletableFuture.completedFuture(null);
        }

        String raw = context.get(blocksArg);
        if (raw == null || raw.trim().isEmpty()) {
            context.sender().sendMessage(Message.raw("Uso: /localradius <numero de blocos>"));
            return CompletableFuture.completedFuture(null);
        }

        int blocks;
        try {
            blocks = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            context.sender().sendMessage(Message.raw("Valor invalido. Use um numero inteiro. Ex: /localradius 80"));
            return CompletableFuture.completedFuture(null);
        }

        plugin.setLocalRadius(blocks);
        context.sender().sendMessage(Message.raw("Raio do chat LOCAL agora e " + plugin.getLocalRadiusInt() + " blocos."));
        return CompletableFuture.completedFuture(null);
    }
}