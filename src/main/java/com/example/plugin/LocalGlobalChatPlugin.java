package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LocalGlobalChatPlugin extends JavaPlugin {

    private static final double LOCAL_RADIUS = 50.0;
    private static final double LOCAL_RADIUS_SQ = LOCAL_RADIUS * LOCAL_RADIUS;

    // TinyMessage/TinyMsg (se estiver instalado no servidor)
    private static final String TINY_GLOBAL = "green";
    private static final String TINY_LOCAL  = "yellow";
    private static final String TINY_TEXT   = "white";

    // Permissão do /chatdebug (admin)
    private static final String PERM_CHATDEBUG = "hytale.command.chatdebug";

    // modo do chat por jogador
    private final Map<UUID, ChatMode> chatModes = new ConcurrentHashMap<>();

    // debug por jogador (toggle /chatdebug)
    private final Map<UUID, Boolean> debugModes = new ConcurrentHashMap<>();

    public LocalGlobalChatPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new GCommand(this));
        getCommandRegistry().registerCommand(new LCommand(this));
        getCommandRegistry().registerCommand(new ChatDebugCommand(this));

        // Registro do chat via reflection (evita erros de generics/assinaturas)
        registerEventListener(PlayerChatEvent.class, ev -> onChat((PlayerChatEvent) ev));

        // (Opcional) tenta resetar para LOCAL quando o jogador entrar (se existir evento)
        tryRegisterJoinResetToLocal();
    }

    // =========================================================
    // Chat local como padrão
    // =========================================================

    private ChatMode getMode(UUID uuid) {
        return chatModes.getOrDefault(uuid, ChatMode.LOCAL);
    }

    private void setMode(UUID uuid, ChatMode mode) {
        chatModes.put(uuid, mode);
    }

    private boolean isDebug(UUID uuid) {
        return debugModes.getOrDefault(uuid, false);
    }

    private void toggleDebug(UUID uuid) {
        debugModes.put(uuid, !isDebug(uuid));
    }

    // =========================================================
    // Registro de eventos via reflection (compatível entre builds)
    // =========================================================

    private void registerEventListener(Class<?> eventClass, Consumer<Object> handler) {
        Object registry = getEventRegistry();
        if (!tryRegister(registry, eventClass, handler)) {
            System.err.println("[LocalGlobalChat] ERRO: nao consegui registrar evento: " + eventClass.getName());
        }
    }

    private boolean tryRegister(Object registry, Class<?> eventClass, Consumer<Object> handler) {
        Method[] methods = registry.getClass().getMethods();

        // 1) register(Class, Consumer)
        try {
            Method m = registry.getClass().getMethod("register", Class.class, Consumer.class);
            m.invoke(registry, eventClass, handler);
            return true;
        } catch (Throwable ignored) { }

        // 2) register(priorityOrShort, Class, Consumer)
        for (Method m : methods) {
            try {
                if (!m.getName().equals("register")) continue;
                if (m.getParameterCount() != 3) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p[1] == Class.class && Consumer.class.isAssignableFrom(p[2])) {
                    Object priorityOrShort = defaultValueFor(p[0]);
                    m.invoke(registry, priorityOrShort, eventClass, handler);
                    return true;
                }
            } catch (Throwable ignored) { }
        }

        // 3) register(Class, key, Consumer)
        for (Method m : methods) {
            try {
                if (!m.getName().equals("register")) continue;
                if (m.getParameterCount() != 3) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p[0] == Class.class && Consumer.class.isAssignableFrom(p[2]) && p[1] != Class.class) {
                    m.invoke(registry, eventClass, null, handler);
                    return true;
                }
            } catch (Throwable ignored) { }
        }

        // 4) register(priorityOrShort, Class, key, Consumer)
        for (Method m : methods) {
            try {
                if (!m.getName().equals("register")) continue;
                if (m.getParameterCount() != 4) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p[1] == Class.class && Consumer.class.isAssignableFrom(p[3]) && p[2] != Class.class) {
                    Object priorityOrShort = defaultValueFor(p[0]);
                    m.invoke(registry, priorityOrShort, eventClass, null, handler);
                    return true;
                }
            } catch (Throwable ignored) { }
        }

        return false;
    }

    private static Object defaultValueFor(Class<?> type) {
        try {
            if (type == short.class || type == Short.class) return (short) 0;
            if (type == int.class || type == Integer.class) return 0;
            if (type == long.class || type == Long.class) return 0L;

            if (type.isEnum()) {
                Object[] values = type.getEnumConstants();
                if (values != null && values.length > 0) return values[0];
            }
        } catch (Throwable ignored) { }
        return null;
    }

    // =========================================================
    // (Opcional) resetar para LOCAL ao entrar (se existir evento)
    // =========================================================

    private void tryRegisterJoinResetToLocal() {
        String[] candidates = new String[] {
                "com.hypixel.hytale.server.core.event.events.player.PlayerJoinEvent",
                "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent",
                "com.hypixel.hytale.server.core.event.events.player.PlayerLoginEvent",
                "com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent"
        };

        for (String cn : candidates) {
            try {
                Class<?> joinEventClass = Class.forName(cn);

                Consumer<Object> handler = ev -> {
                    PlayerRef p = extractPlayerRef(ev);
                    if (p != null && p.getUuid() != null) {
                        setMode(p.getUuid(), ChatMode.LOCAL);
                    }
                };

                if (tryRegister(getEventRegistry(), joinEventClass, handler)) {
                    System.out.println("[LocalGlobalChat] JoinEvent registrado: " + cn);
                    return;
                }
            } catch (Throwable ignored) { }
        }

        System.out.println("[LocalGlobalChat] JoinEvent nao encontrado (ok). Default LOCAL ainda funciona.");
    }

    private static PlayerRef extractPlayerRef(Object event) {
        String[] methods = {"getPlayer", "getSender", "player", "sender"};
        for (String mname : methods) {
            try {
                Method m = event.getClass().getMethod(mname);
                Object r = m.invoke(event);
                if (r instanceof PlayerRef) return (PlayerRef) r;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    // =========================================================
    // Chat formatado (cores via TinyMsg se instalado)
    // =========================================================

    private void onChat(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();
        UUID senderUuid = sender.getUuid();

        ChatMode mode = getMode(senderUuid);

        event.setFormatter((ignoredViewer, message) -> formatChat(mode, sender.getUsername(), message));

        // Local: filtra targets por distância
        if (mode == ChatMode.LOCAL) {
            UUID senderWorld = sender.getWorldUuid();

            double x0 = getX(sender);
            double y0 = getY(sender);
            double z0 = getZ(sender);

            event.getTargets().removeIf(target -> {
                if (target == null) return true;
                if (!Objects.equals(senderWorld, target.getWorldUuid())) return true;

                double dx = getX(target) - x0;
                double dy = getY(target) - y0;
                double dz = getZ(target) - z0;

                return (dx * dx + dy * dy + dz * dz) > LOCAL_RADIUS_SQ;
            });
        }

        // DEBUG (somente se estiver ligado)
        if (isDebug(senderUuid)) {
            StringBuilder sb = new StringBuilder();
            sb.append("DEBUG chat=").append(mode == ChatMode.LOCAL ? "LOCAL" : "GLOBAL");
            sb.append(" targets=").append(event.getTargets().size()).append(" -> ");

            int shown = 0;
            for (PlayerRef t : event.getTargets()) {
                if (t == null) continue;
                sb.append(t.getUsername()).append(", ");
                shown++;
                if (shown >= 10) {
                    sb.append("...");
                    break;
                }
            }
            sender.sendMessage(Message.raw(sb.toString()));
        }
    }

    private static Message formatChat(ChatMode mode, String username, String msg) {
        String tag = (mode == ChatMode.LOCAL) ? "[L] " : "[G] ";
        String col = (mode == ChatMode.LOCAL) ? TINY_LOCAL : TINY_GLOBAL;

        // evita jogador quebrar tags usando "<...>"
        String safeUser = tinySafe(username);
        String safeMsg  = tinySafe(msg);

        // Texto TinyMsg (se o mod estiver instalado)
        String tiny =
                "<color:" + col + ">" + tag + safeUser + "</color>" +
                        "<color:" + TINY_TEXT + ">: " + safeMsg + "</color>";

        // fallback sem cor
        String plain = tag + username + ": " + msg;

        Message parsed = tryTinyMsgParse(tiny);
        return (parsed != null) ? parsed : Message.raw(plain);
    }

    private static String tinySafe(String s) {
        if (s == null) return "";
        return s.replace("<", "‹").replace(">", "›");
    }

    @Nullable
    private static Message tryTinyMsgParse(String tinyText) {
        // TinyMessage/TinyMsg: fi.sulku.hytale.TinyMsg.parse(String) -> Message
        try {
            Class<?> tinyMsg = Class.forName("fi.sulku.hytale.TinyMsg");
            Method parse = tinyMsg.getMethod("parse", String.class);
            Object out = parse.invoke(null, tinyText);
            if (out instanceof Message) return (Message) out;
        } catch (Throwable ignored) { }
        return null;
    }

    // =========================================================
    // Posição (reflection pra suportar builds diferentes)
    // =========================================================

    private static double getX(PlayerRef p) { return getAxis(p, "x"); }
    private static double getY(PlayerRef p) { return getAxis(p, "y"); }
    private static double getZ(PlayerRef p) { return getAxis(p, "z"); }

    private static double getAxis(PlayerRef p, String axis) {
        try {
            Object transform = p.getTransform();

            Object pos = invokeAny(transform,
                    "getPosition", "position",
                    "getTranslation", "translation",
                    "getLocation", "location"
            );

            Object base = (pos != null) ? pos : transform;

            Object value = invokeAny(base,
                    "get" + axis.toUpperCase(), axis
            );

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Throwable ignored) { }

        return 0.0;
    }

    private static Object invokeAny(Object obj, String... methodNames) {
        if (obj == null) return null;
        for (String name : methodNames) {
            try {
                Method m = obj.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    // =========================================================
    // Permissões: /g e /l livres (fallback via reflection)
    // =========================================================

    private static void relaxCommandPermissions(Object cmd) {
        String[] methodNames = {
                "setRequiredPermissionLevel",
                "setMinPermissionLevel",
                "setPermissionLevel",
                "setPermission",
                "setPermissionNode",
                "setRequiredPermission",
                "requirePermission"
        };

        for (String mname : methodNames) {
            try {
                Method m = cmd.getClass().getMethod(mname, int.class);
                m.invoke(cmd, 0);
            } catch (Throwable ignored) { }
            try {
                Method m = cmd.getClass().getMethod(mname, short.class);
                m.invoke(cmd, (short) 0);
            } catch (Throwable ignored) { }
            try {
                Method m = cmd.getClass().getMethod(mname, String.class);
                // mais seguro: null (muitos checks fazem "if (perm != null)")
                m.invoke(cmd, new Object[]{null});
            } catch (Throwable ignored) { }
        }

        String[] fieldNames = {
                "requiredPermissionLevel",
                "minPermissionLevel",
                "permissionLevel",
                "permission",
                "permissionNode",
                "requiredPermission"
        };

        for (String fname : fieldNames) {
            try {
                Field f = cmd.getClass().getDeclaredField(fname);
                f.setAccessible(true);

                if (f.getType() == int.class || f.getType() == Integer.class) f.set(cmd, 0);
                else if (f.getType() == short.class || f.getType() == Short.class) f.set(cmd, (short) 0);
                else if (f.getType() == String.class) f.set(cmd, null);
            } catch (Throwable ignored) { }
        }
    }

    // =========================================================
    // /chatdebug (admin): exigir permissão por node (reflection)
    // =========================================================

    private static void requirePermissionNode(Object cmd, String node) {
        String[] names = {
                "setPermissionNode",
                "setPermission",
                "setRequiredPermission",
                "setRequiredPermissionNode",
                "requirePermission"
        };

        for (String n : names) {
            try {
                Method m = cmd.getClass().getMethod(n, String.class);
                m.invoke(cmd, node);
                return;
            } catch (Throwable ignored) { }
        }

        // fallback: fields
        String[] fields = {"permissionNode", "permission", "requiredPermission"};
        for (String fName : fields) {
            try {
                Field f = cmd.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                if (f.getType() == String.class) {
                    f.set(cmd, node);
                    return;
                }
            } catch (Throwable ignored) { }
        }
    }

    // ✅ agora aceita Object (CommandSender ou PlayerRef)
    private static boolean hasPermissionCompat(Object sender, String node) {
        if (sender == null) return false;

        String[] names = {
                "hasPermission",
                "hasPermissionNode",
                "hasPerm",
                "permission"
        };

        for (String n : names) {
            try {
                Method m = sender.getClass().getMethod(n, String.class);
                Object r = m.invoke(sender, node);
                if (r instanceof Boolean) return (Boolean) r;
            } catch (Throwable ignored) { }
        }

        // Se não existe API de checagem nesse build, deixa o servidor decidir via permission node do comando
        return true;
    }

    // =========================================================
    // Modos + comandos
    // =========================================================

    private enum ChatMode {
        GLOBAL, LOCAL
    }

    // -------------------------
    // /g (livre)
    // -------------------------
    private static final class GCommand extends AbstractCommand {
        private final LocalGlobalChatPlugin plugin;

        private GCommand(LocalGlobalChatPlugin plugin) {
            super("g", "Alterna para o chat GLOBAL");
            this.plugin = plugin;

            // libera por reflection (fallback)
            relaxCommandPermissions(this);
        }

        @Override
        protected boolean canGeneratePermission() {
            // ✅ comando livre
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

    // -------------------------
    // /l (livre)
    // -------------------------
    private static final class LCommand extends AbstractCommand {
        private final LocalGlobalChatPlugin plugin;

        private LCommand(LocalGlobalChatPlugin plugin) {
            super("l", "Alterna para o chat LOCAL (raio de 50 blocos)");
            this.plugin = plugin;

            // libera por reflection (fallback)
            relaxCommandPermissions(this);
        }

        @Override
        protected boolean canGeneratePermission() {
            // ✅ comando livre
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
            context.sender().sendMessage(Message.raw("Agora voce esta no chat LOCAL (50 blocos). [L]"));
            return CompletableFuture.completedFuture(null);
        }
    }

    // -------------------------
    // /chatdebug (admin)
    // -------------------------
    private static final class ChatDebugCommand extends AbstractCommand {
        private final LocalGlobalChatPlugin plugin;

        private ChatDebugCommand(LocalGlobalChatPlugin plugin) {
            super("chatdebug", "Ativa/desativa debug do chat (mostra targets)");
            this.plugin = plugin;

            // ✅ exige permissão (admin)
            requirePermissionNode(this, PERM_CHATDEBUG);
        }

        @Override
        protected boolean canGeneratePermission() {
            // ✅ servidor controla quem pode (admin)
            return true;
        }

        @Override
        @Nullable
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            UUID uuid = context.sender().getUuid();
            if (uuid == null) {
                context.sender().sendMessage(Message.raw("Este comando so pode ser usado por jogadores."));
                return CompletableFuture.completedFuture(null);
            }

            // ✅ checagem extra (sem erro de tipo agora)
            if (!hasPermissionCompat(context.sender(), PERM_CHATDEBUG)) {
                context.sender().sendMessage(Message.raw("Voce nao tem permissao para usar /chatdebug."));
                return CompletableFuture.completedFuture(null);
            }

            plugin.toggleDebug(uuid);
            boolean now = plugin.isDebug(uuid);

            context.sender().sendMessage(Message.raw("ChatDebug: " + (now ? "ON" : "OFF")));
            return CompletableFuture.completedFuture(null);
        }
    }
}
