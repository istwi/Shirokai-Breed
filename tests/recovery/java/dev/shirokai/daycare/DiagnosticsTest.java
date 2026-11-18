package dev.shirokai.daycare;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;

/** Standalone diagnostics regression; uses a temporary config, not user files. */
public class DiagnosticsTest {
    public static void main(String[] args) throws Exception {
        Path config=Files.createTempDirectory("zorykai-logs-test-");
        Files.writeString(config.resolve("diagnostics.json"),"{\"enabled\":true,\"maxFileBytes\":65536,\"retentionDays\":1}");
        Path logs=config.resolve("logs");Files.createDirectories(logs);
        Files.writeString(logs.resolve("daycare-2000-01-01.jsonl"),"old\n");
        Files.writeString(logs.resolve("keep-user-file.txt"),"preserve\n");
        RecoveryLog.install(config);
        UUID player=UUID.randomUUID(),operation=UUID.randomUUID();
        RecoveryLog.write("ADMIN_RECOVER_REQUEST",player,operation,5,"marker");
        LogManager.getLogger("shirokai-daycare").error("diagnostic-error-marker",new IllegalStateException("test-stack-marker"));
        for(int i=0;i<400;i++) RecoveryLog.write("ROTATION_TEST",player,operation,0,"x".repeat(400));
        if(Files.exists(logs.resolve("daycare-2000-01-01.jsonl")))throw new AssertionError("Old diagnostics not removed");
        if(!Files.exists(logs.resolve("keep-user-file.txt")))throw new AssertionError("Unrelated user file removed");
        int rows=0,files=0;boolean recover=false,error=false,rotated=false;
        try(var paths=Files.list(logs)) {
            for(Path path:paths.filter(p->p.toString().endsWith(".jsonl")).toList()) {
                files++;rotated|=path.getFileName().toString().contains(".1.");
                for(String line:Files.readAllLines(path)) {
                    var row=JsonParser.parseString(line).getAsJsonObject();rows++;
                    if(!row.has("timestamp_utc")||!row.has("action"))throw new AssertionError("Invalid record");
                    if(row.get("action").getAsString().equals("ADMIN_RECOVER_REQUEST")) {
                        recover=row.get("player_uuid").getAsString().equals(player.toString())&&row.get("operation_uuid").getAsString().equals(operation.toString())&&row.get("breed_slot").getAsInt()==6;
                    }
                    if(row.has("stacktrace"))error|=row.get("stacktrace").getAsString().contains("test-stack-marker");
                }
            }
        }
        if(!recover||!error||!rotated||rows!=403||files<2)throw new AssertionError("Incomplete logging: rows="+rows+" recover="+recover+" error="+error+" rotated="+rotated);
        System.out.println("PASS: 403 JSON records; player/operation/slot, exception capture, size rotation, retention, unrelated-file preservation");
    }
}
