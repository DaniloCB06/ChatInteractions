package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MsgCommand extends AbstractCommand {

    private final RequiredArg<PlayerRef> targetArg;
    private final RequiredArg<String> messageArg;

    public MsgCommand() {
        super("msg", "Envia uma mensagem privada para um jogador");

        // comando livre
        LGChatCompat.relaxCommandPermissions(this);

        targetArg = withRequiredArg("player", "Jogador alvo", ArgTypes.PLAYER_REF);
        messageArg = withRequiredArg("message", "Mensagem", ArgTypes.STRING);

        tryMakeArgGreedy(messageArg);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        UUID senderUuid = context.sender().getUuid();
        if (senderUuid == null) {
            context.sender().sendMessage(LGChatCompat.pinkMessage("Este comando so pode ser usado por jogadores."));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef target = context.get(targetArg);
        if (target == null || target.getUuid() == null) {
            context.sender().sendMessage(LGChatCompat.pinkMessage("Jogador nao encontrado (online)."));
            return CompletableFuture.completedFuture(null);
        }

        if (Objects.equals(senderUuid, target.getUuid())) {
            context.sender().sendMessage(LGChatCompat.pinkMessage("Voce nao pode enviar /msg para voce mesmo."));
            return CompletableFuture.completedFuture(null);
        }

        String msg = context.get(messageArg);

        String remainder = extractMsgRemainderFromInput(getInputStringCompat(context));
        if (remainder != null && !remainder.isBlank()) {
            msg = remainder.trim();
        }

        if (msg == null || msg.trim().isEmpty()) {
            context.sender().sendMessage(LGChatCompat.pinkMessage("Uso: /msg <player> <mensagem...>"));
            return CompletableFuture.completedFuture(null);
        }

        String senderName = LGChatCompat.resolveSenderUsername(context.sender(), senderUuid);

        context.sender().sendMessage(LGChatCompat.pinkMessage("[To " + target.getUsername() + "] " + msg));
        target.sendMessage(LGChatCompat.pinkMessage("[from " + senderName + "] " + msg));

        return CompletableFuture.completedFuture(null);
    }

    private static void tryMakeArgGreedy(Object arg) {
        if (arg == null) return;

        String[] methodsNoArgs = {"greedy", "consumeRemaining", "consumeRest", "takeRemaining", "readRemaining"};
        for (String mn : methodsNoArgs) {
            try {
                Method m = arg.getClass().getMethod(mn);
                m.invoke(arg);
                return;
            } catch (Throwable ignored) { }
        }

        String[] methodsBool = {"setGreedy", "setConsumeRemaining", "setConsumeRest", "setTakeRemaining", "setReadRemaining"};
        for (String mn : methodsBool) {
            try {
                Method m = arg.getClass().getMethod(mn, boolean.class);
                m.invoke(arg, true);
                return;
            } catch (Throwable ignored) { }
        }
    }

    @Nullable
    private static String getInputStringCompat(CommandContext context) {
        if (context == null) return null;

        for (String mn : new String[]{"getInputString", "getInput", "input", "getRaw", "raw", "getCommandLine", "commandLine"}) {
            try {
                Method m = context.getClass().getMethod(mn);
                Object r = m.invoke(context);
                if (r instanceof String s) return s;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    @Nullable
    private static String extractMsgRemainderFromInput(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.startsWith("/")) s = s.substring(1);

        String[] parts = s.split("\\s+", 3);
        if (parts.length < 3) return null;

        if (!parts[0].equalsIgnoreCase("msg")) return null;
        return parts[2];
    }
}