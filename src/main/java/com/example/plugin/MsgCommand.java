package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MsgCommand extends AbstractCommand {

    private final RequiredArg<PlayerRef> targetArg;
    private final RequiredArg<String> messageArg;

    public MsgCommand() {
        super("msg", "Envia uma mensagem privada para um jogador");

        LGChatCompat.relaxCommandPermissions(this);

        targetArg = withRequiredArg("player", "Jogador alvo", ArgTypes.PLAYER_REF);

        // 1) tenta encontrar um ArgType “greedy/rest-of-line” dentro de ArgTypes
        Object bestMsgType = findGreedyLikeTextArgType();
        RequiredArg<String> tmp;

        if (bestMsgType != null && bestMsgType != ArgTypes.STRING) {
            tmp = withRequiredArgCompat("message", "Mensagem", bestMsgType);
        } else {
            tmp = withRequiredArg("message", "Mensagem", ArgTypes.STRING);
        }
        messageArg = tmp;

        // 2) tenta ligar greedy no argumento/tipo (caso exista essa flag no build)
        tryEnableGreedyRecursively(messageArg);
        tryEnableGreedyRecursively(bestMsgType);

        // 3) MAIS IMPORTANTE: força o comando a aceitar argumentos “a mais”
        forceAllowTrailingArguments(this);
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

        // Preferência TOTAL: pega o resto da linha crua (funciona mesmo se o parser só consumir 1 palavra)
        String raw = getInputStringCompat(context);
        String remainder = extractRemainder(raw);

        String msg;
        if (remainder != null && !remainder.isBlank()) {
            msg = remainder.trim();
        } else {
            // fallback: o que o parser deu
            msg = context.get(messageArg);
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

    // ------------------------------------------------------------
    //  A) Fallback robusto: extrair "<mensagem...>" do input cru
    // ------------------------------------------------------------
    @Nullable
    private static String extractRemainder(String input) {
        if (input == null) return null;

        String s = input.trim();
        if (s.startsWith("/")) s = s.substring(1);

        // <cmd> <player> <resto...>
        String[] parts = s.split("\\s+", 3);
        if (parts.length < 3) return null;

        // não amarra no nome "msg" pra ser mais tolerante (aliases etc.)
        return parts[2];
    }

    @Nullable
    private static String getInputStringCompat(CommandContext context) {
        if (context == null) return null;

        for (String mn : new String[]{"getInputString","getInput","input","getRaw","raw","getCommandLine","commandLine"}) {
            try {
                Method m = context.getClass().getMethod(mn);
                Object r = m.invoke(context);
                if (r instanceof String s) return s;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    // ------------------------------------------------------------
    //  B) Criar RequiredArg usando argType descoberto via reflection
    // ------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private RequiredArg<String> withRequiredArgCompat(String name, String desc, Object argType) {
        if (argType == null) return withRequiredArg(name, desc, ArgTypes.STRING);

        try {
            for (Class<?> c = getClass(); c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (!m.getName().equals("withRequiredArg")) continue;
                    if (m.getParameterCount() != 3) continue;

                    Class<?>[] p = m.getParameterTypes();
                    if (p[0] != String.class || p[1] != String.class) continue;
                    if (!p[2].isAssignableFrom(argType.getClass())) continue;

                    m.setAccessible(true);
                    Object r = m.invoke(this, name, desc, argType);
                    if (r instanceof RequiredArg) return (RequiredArg<String>) r;
                }
            }
        } catch (Throwable ignored) { }

        return withRequiredArg(name, desc, ArgTypes.STRING);
    }

    // ------------------------------------------------------------
    //  C) Achar um ArgTypes “greedy/rest/message” mesmo que o nome não seja óbvio
    // ------------------------------------------------------------
    private static Object findGreedyLikeTextArgType() {
        Object base = ArgTypes.STRING;
        if (base == null) return null;

        Class<?> baseClass = base.getClass();
        Object best = base;
        int bestScore = -1;

        try {
            for (Field f : ArgTypes.class.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!baseClass.isAssignableFrom(f.getType())) continue;

                Object v = f.get(null);
                if (v == null) continue;

                String fn = f.getName().toLowerCase(Locale.ROOT);
                String cn = v.getClass().getName().toLowerCase(Locale.ROOT);
                String ts = String.valueOf(v).toLowerCase(Locale.ROOT);

                int score = 0;
                score += scoreWord(fn, "greedy", 50);
                score += scoreWord(fn, "remain", 40);
                score += scoreWord(fn, "rest", 40);
                score += scoreWord(fn, "remainder", 40);
                score += scoreWord(fn, "message", 30);
                score += scoreWord(fn, "chat", 20);
                score += scoreWord(fn, "text", 5);

                score += scoreWord(cn, "greedy", 50);
                score += scoreWord(cn, "remain", 40);
                score += scoreWord(cn, "rest", 40);
                score += scoreWord(cn, "remainder", 40);
                score += scoreWord(cn, "message", 30);

                score += scoreWord(ts, "greedy", 20);
                score += scoreWord(ts, "remain", 15);
                score += scoreWord(ts, "rest", 15);
                score += scoreWord(ts, "message", 10);

                // bônus: se tiver isGreedy() e retornar true
                Boolean isGreedy = tryCallBoolean(v, "isGreedy", "greedy");
                if (Boolean.TRUE.equals(isGreedy)) score += 100;

                if (score > bestScore) {
                    bestScore = score;
                    best = v;
                }
            }
        } catch (Throwable ignored) { }

        return best;
    }

    private static int scoreWord(String s, String w, int pts) {
        return (s != null && s.contains(w)) ? pts : 0;
    }

    private static Boolean tryCallBoolean(Object obj, String... names) {
        if (obj == null) return null;
        for (String n : names) {
            try {
                Method m = obj.getClass().getMethod(n);
                Object r = m.invoke(obj);
                if (r instanceof Boolean b) return b;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    // ------------------------------------------------------------
    //  D) Ativar modo greedy/pro “rest of line” (tentando em objetos internos)
    // ------------------------------------------------------------
    private static void tryEnableGreedyRecursively(Object root) {
        if (root == null) return;
        tryEnableGreedy(root);

        // tenta em campos internos (1 nível) — sem fazer varredura profunda pra não ser perigoso
        for (Field f : root.getClass().getDeclaredFields()) {
            try {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object inner = f.get(root);
                if (inner == null) continue;

                // evita recursão inútil em tipos muito simples
                String cn = inner.getClass().getName();
                if (cn.startsWith("java.") || cn.startsWith("javax.")) continue;

                tryEnableGreedy(inner);
            } catch (Throwable ignored) { }
        }
    }

    private static void tryEnableGreedy(Object obj) {
        if (obj == null) return;

        for (Method m : obj.getClass().getMethods()) {
            try {
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (m.getReturnType() != void.class) continue;

                boolean looks =
                        n.contains("greedy") ||
                                n.contains("consumeremaining") ||
                                n.contains("consumerest") ||
                                n.contains("takeremaining") ||
                                n.contains("readremaining") ||
                                n.contains("restofline") ||
                                n.contains("remainder");

                if (!looks) continue;

                if (m.getParameterCount() == 0) {
                    m.invoke(obj);
                    return;
                }
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    m.invoke(obj, true);
                    return;
                }
            } catch (Throwable ignored) { }
        }
    }

    // ------------------------------------------------------------
    //  E) FORÇAR aceitar argumentos extras (isso é o que destrava o “Expected 2, actual 7”)
    // ------------------------------------------------------------
    private static void forceAllowTrailingArguments(Object cmd) {
        if (cmd == null) return;

        // 1) tenta métodos comuns (mas agora bem mais flexível)
        for (Method m : cmd.getClass().getMethods()) {
            try {
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (m.getReturnType() != void.class) continue;

                boolean aboutArgs =
                        n.contains("arg") || n.contains("argument") || n.contains("parameter") || n.contains("params");

                boolean allowExtra =
                        n.contains("allow") && (n.contains("extra") || n.contains("trailing") || n.contains("unknown") || n.contains("additional") || n.contains("more"));

                boolean ignoreExtra =
                        n.contains("ignore") && (n.contains("extra") || n.contains("trailing") || n.contains("unknown") || n.contains("additional") || n.contains("more"));

                boolean notStrict =
                        (n.contains("strict") || n.contains("exact") || n.contains("enforce")) && aboutArgs;

                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    if (allowExtra || ignoreExtra) {
                        m.invoke(cmd, true);
                        return;
                    }
                    if (notStrict) {
                        m.invoke(cmd, false);
                        return;
                    }
                }

                if (m.getParameterCount() == 0) {
                    if (allowExtra || ignoreExtra) {
                        m.invoke(cmd);
                        return;
                    }
                }
            } catch (Throwable ignored) { }
        }

        // 2) se não achou método, seta fields prováveis no comando/superclasses
        for (Class<?> c = cmd.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(f.getModifiers())) continue;

                    String n = f.getName().toLowerCase(Locale.ROOT);

                    boolean aboutArgs =
                            n.contains("arg") || n.contains("argument") || n.contains("parameter") || n.contains("params");

                    boolean likelyAllowExtra =
                            aboutArgs && (n.contains("extra") || n.contains("trailing") || n.contains("unknown") || n.contains("additional") || n.contains("more"));

                    boolean likelyStrict =
                            aboutArgs && (n.contains("strict") || n.contains("exact") || n.contains("enforce"));

                    f.setAccessible(true);

                    if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                        if (likelyAllowExtra) {
                            f.set(cmd, true);
                            return;
                        }
                        if (likelyStrict) {
                            f.set(cmd, false);
                            return;
                        }
                    }

                    if (aboutArgs && (f.getType() == int.class || f.getType() == Integer.class)) {
                        if (n.contains("max")) {
                            f.set(cmd, 9999);
                            // não dá return aqui: pode existir também um boolean junto
                        }
                    }
                } catch (Throwable ignored) { }
            }
        }
    }
}
