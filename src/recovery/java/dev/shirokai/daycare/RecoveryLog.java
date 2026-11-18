package dev.shirokai.daycare;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.shirokai.daycare.domain.TrainerState;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/** Local diagnostics. Never logs Pokémon snapshots, inventory, IPs or credentials. */
public final class RecoveryLog {
    private static final Gson JSON = new Gson();
    private static Path directory;
    private static boolean warned;
    private static boolean enabled = true;
    private static long maxBytes = 10 * 1024 * 1024;
    private static int retentionDays = 14;
    private static LocalDate lastCleanup;
    private RecoveryLog() {}

    public static synchronized void install(Path config) {
        try {
            Path settings = config.resolve("diagnostics.json");
            Files.createDirectories(config);
            if (!Files.exists(settings)) {
                Files.writeString(settings, "{\n  \"enabled\": true,\n  \"maxFileBytes\": 10485760,\n  \"retentionDays\": 14\n}\n", StandardOpenOption.CREATE_NEW);
            }
            JsonObject data = JSON.fromJson(Files.readString(settings), JsonObject.class);
            enabled = data.has("enabled") ? data.get("enabled").getAsBoolean() : true;
            maxBytes = data.has("maxFileBytes") ? data.get("maxFileBytes").getAsLong() : maxBytes;
            retentionDays = data.has("retentionDays") ? data.get("retentionDays").getAsInt() : retentionDays;
            if (maxBytes < 65536 || maxBytes > 100 * 1024 * 1024 || retentionDays < 1 || retentionDays > 365) throw new IllegalArgumentException("Limites de diagnostics.json inválidos");
            directory = config.resolve("logs");
            if (!enabled) return;
            Files.createDirectories(directory);
            var logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            if (!logger.getAppenders().containsKey("ZorykaiLocalDiagnostics")) {
                AbstractAppender appender = new AbstractAppender("ZorykaiLocalDiagnostics", null, null, true, Property.EMPTY_ARRAY) {
                    public void append(LogEvent event) {
                        if (!"shirokai-daycare".equals(event.getLoggerName()) && !daycareFailure(event.getThrown())) return;
                        JsonObject row = base("SERVER_LOG", null, null, null);
                        row.addProperty("level", event.getLevel().toString());
                        row.addProperty("source_logger", event.getLoggerName());
                        row.addProperty("message", event.getMessage().getFormattedMessage());
                        if (event.getThrown() != null) {
                            StringWriter trace = new StringWriter(); event.getThrown().printStackTrace(new PrintWriter(trace));
                            String text = trace.toString(); row.addProperty("stacktrace", text.substring(0, Math.min(text.length(), 32768)));
                        }
                        appendRow(row);
                    }
                };
                appender.start(); logger.addAppender(appender);
            }
            write("DIAGNOSTICS_STARTED", null, null, null, "Local diagnostics active; times in UTC");
        } catch (Exception e) { warn(e); }
    }

    private static JsonObject base(String action, UUID owner, UUID operation, Integer slot) {
        JsonObject row = new JsonObject();
        row.addProperty("timestamp_utc", Instant.now().toString());
        row.addProperty("action", action);
        if (owner != null) row.addProperty("player_uuid", owner.toString());
        if (operation != null) row.addProperty("operation_uuid", operation.toString());
        if (slot != null && slot >= 0) row.addProperty("breed_slot", slot + 1);
        return row;
    }

    private static boolean daycareFailure(Throwable error) {
        for (int depth=0;error!=null&&depth<8;depth++,error=error.getCause()) {
            for (StackTraceElement frame:error.getStackTrace()) if(frame.getClassName().startsWith("dev.shirokai.daycare.")) return true;
        }
        return false;
    }

    public static void write(String action, UUID owner, UUID operation, Integer slot, String message) {
        JsonObject row = base(action, owner, operation, slot);
        if (message != null) row.addProperty("message", message);
        appendRow(row);
    }

    public static void audit(TrainerState state, String event, UUID operation) {
        if (event.equals("CHECKPOINT")) return;
        Integer slot = state.operation != null && state.operation.kind != null && java.util.Set.of("DEPOSIT", "DEPOSIT_PARENT", "WITHDRAW", "WITHDRAW_PARENT", "RESET_SLOT").contains(state.operation.kind) ? state.operation.slot : null;
        write("JOURNAL_WRITE_REQUEST", state.owner, operation, slot, event);
    }

    private static synchronized void appendRow(JsonObject row) {
        if (!enabled || directory == null) return;
        try {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            if (!today.equals(lastCleanup)) {
                try (var files = Files.list(directory)) {
                    for (Path file : files.toList()) {
                        String name = file.getFileName().toString();
                        if (!name.matches("daycare-\\d{4}-\\d{2}-\\d{2}(\\.[1-5])?\\.jsonl")) continue;
                        LocalDate day = LocalDate.parse(name.substring(8, 18));
                        if (day.isBefore(today.minusDays(retentionDays))) Files.deleteIfExists(file);
                    }
                }
                lastCleanup = today;
            }
            Path file = directory.resolve("daycare-" + today + ".jsonl");
            byte[] bytes = (JSON.toJson(row) + "\n").getBytes(StandardCharsets.UTF_8);
            if (Files.exists(file) && Files.size(file) + bytes.length > maxBytes) {
                for (int i = 5; i >= 1; i--) {
                    Path to = directory.resolve("daycare-" + today + "." + i + ".jsonl");
                    Path from = i == 1 ? file : directory.resolve("daycare-" + today + "." + (i - 1) + ".jsonl");
                    if (Files.exists(from)) Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) { warn(e); }
    }

    private static void warn(Exception error) {
        if (!warned) { warned = true; System.err.println("[Zorykai] Diagnóstico local indisponível; consulte o log do servidor: " + error.getClass().getSimpleName()); }
    }
}
