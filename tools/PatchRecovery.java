import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Applies reviewed recovery sources to the distributed binary, preserving all other entries. */
public class PatchRecovery implements Opcodes {
    static final String P = "dev/shirokai/daycare/";
    static final String S = P + "DaycareService";
    static final String T = P + "domain/TrainerState";
    static final String G = P + "integration/CobblemonGateway";
    static final String R = P + "SlotRecovery";
    static int schemaBounds, schemaWrites, registrations;

    static AbstractInsnNode previous(AbstractInsnNode n) {
        do { n = n.getPrevious(); } while (n != null && n.getOpcode() < 0);
        return n;
    }

    static void body(MethodNode m, java.util.function.Consumer<MethodNode> emitter) {
        m.instructions.clear(); m.tryCatchBlocks.clear();
        if (m.localVariables != null) m.localVariables.clear();
        m.visibleLocalVariableAnnotations = null; m.invisibleLocalVariableAnnotations = null;
        emitter.accept(m); m.visitMaxs(0, 0);
    }

    static MethodNode getter(ClassNode node, String method, String field, String desc) {
        MethodNode m = new MethodNode(ASM9, ACC_PUBLIC, method, "()" + desc, null, null);
        m.visitVarInsn(ALOAD, 0); m.visitFieldInsn(GETFIELD, node.name, field, desc); m.visitInsn(ARETURN); m.visitMaxs(0, 0);
        node.methods.add(m); return m;
    }

    static void startLog(MethodNode method, String action, boolean player, Integer slotLocal) {
        InsnList in = new InsnList();
        in.add(new LdcInsnNode(action)); in.add(new VarInsnNode(ALOAD, 1));
        if (player) in.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/class_3222", "method_5667", "()Ljava/util/UUID;", false));
        in.add(new InsnNode(ACONST_NULL));
        if (slotLocal == null) in.add(new InsnNode(ACONST_NULL));
        else { in.add(new VarInsnNode(ILOAD, slotLocal)); in.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)); }
        in.add(new InsnNode(ACONST_NULL));
        in.add(new MethodInsnNode(INVOKESTATIC, P + "RecoveryLog", "write", "(Ljava/lang/String;Ljava/util/UUID;Ljava/util/UUID;Ljava/lang/Integer;Ljava/lang/String;)V", false));
        method.instructions.insert(in);
    }

    static byte[] patch(String entry, byte[] bytes) {
        ClassNode c = new ClassNode(); new ClassReader(bytes).accept(c, 0);
        // Schema 4 prevents an older JAR from misinterpreting the new RESET_SLOT journal.
        for (MethodNode m : c.methods) for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n instanceof LdcInsnNode text) {
                if ("Destino: §fequipe ou PC".equals(text.cst)) text.cst = "Destino: §fPC";
                if ("Sem espaço, o ovo continua aqui.".equals(text.cst)) text.cst = "Sem espaço no PC, o ovo continua aqui.";
                if ("Equipe e PC sem espaço. O Pokémon permanece guardado.".equals(text.cst)) text.cst = "PC sem espaço. O Pokémon permanece guardado.";
            }
            if (n instanceof FieldInsnNode f && f.owner.equals(T) && f.name.equals("schema") && f.getOpcode() == PUTFIELD) {
                AbstractInsnNode before = previous(n);
                if (before.getOpcode() == ICONST_3) { m.instructions.set(before, new InsnNode(ICONST_4)); schemaWrites++; }
            }
            if (n.getOpcode() == IF_ICMPGT && previous(n).getOpcode() == ICONST_3) {
                AbstractInsnNode field = previous(previous(n));
                if (field instanceof FieldInsnNode f && f.owner.equals(T) && f.name.equals("schema")) {
                    m.instructions.set(previous(n), new InsnNode(ICONST_4)); schemaBounds++;
                }
            }
        }

        if(c.name.equals(P+"config/Settings")) for(MethodNode m:c.methods) if(m.name.equals("load")&&m.desc.equals("(Ljava/nio/file/Path;)L"+P+"config/Settings;")) {
            for(AbstractInsnNode n:m.instructions.toArray()) if(n.getOpcode()==ARETURN) {
                InsnList in=new InsnList();in.add(new InsnNode(DUP));in.add(new MethodInsnNode(INVOKESTATIC,P+"PcPolicy","configure","(L"+P+"config/Settings;)V",false));m.instructions.insertBefore(n,in);
            }
        }

        if (c.name.equals(G)) {
            for (MethodNode m : c.methods) if (Set.of("find", "capacity", "remove", "deliver").contains(m.name)) {
                String method = m.name, desc = m.desc;
                body(m, out -> {
                    out.visitVarInsn(ALOAD, 0); out.visitVarInsn(ALOAD, 1);
                    out.visitVarInsn(method.equals("capacity") ? ILOAD : ALOAD, 2);
                    out.visitMethodInsn(INVOKESTATIC, P + "StorageSafety", method, "(L" + G + ";" + desc.substring(1), false);
                    out.visitInsn(method.equals("remove") ? RETURN : method.equals("find") ? ARETURN : IRETURN);
                });
            }
        }

        if (c.name.equals(S)) {
            c.interfaces.add(P + "RecoveryAccess");
            getter(c, "recoveryBusy", "busy", "Ljava/util/Set;");
            getter(c, "recoveryFailed", "failed", "Ljava/util/Set;");
            getter(c, "recoveryFixtures", "integrationOwners", "Ljava/util/Set;");
            getter(c, "recoveryFailAfterDurable", "failAfterDurable", "Ljava/util/Set;");
            String desc = "(L" + T + ";Ljava/lang/String;Ljava/util/UUID;Ljava/lang/Runnable;)V";
            MethodNode bridge = new MethodNode(ASM9, ACC_PUBLIC, "recoveryPersist", desc, null, null);
            for (int i = 0; i <= 4; i++) bridge.visitVarInsn(ALOAD, i);
            bridge.visitMethodInsn(INVOKEVIRTUAL, S, "persist", desc, false);bridge.visitInsn(RETURN);bridge.visitMaxs(0, 0);c.methods.add(bridge);
            for (MethodNode m : c.methods) {
                if (m.name.equals("withdraw")) body(m, out -> {
                    out.visitVarInsn(ALOAD,0);out.visitVarInsn(ALOAD,1);out.visitVarInsn(ILOAD,2);
                    out.visitMethodInsn(INVOKESTATIC,R,"withdrawPair","(L"+S+";Lnet/minecraft/class_3222;I)V",false);out.visitInsn(RETURN);
                });
                if (m.name.equals("recover")) {
                    LabelNode original = new LabelNode(); InsnList in = new InsnList();
                    in.add(new VarInsnNode(ALOAD, 0));in.add(new VarInsnNode(ALOAD, 1));
                    in.add(new MethodInsnNode(INVOKESTATIC, R, "recover", "(L" + S + ";Ljava/util/UUID;)Z", false));
                    in.add(new JumpInsnNode(IFEQ, original));in.add(new InsnNode(RETURN));in.add(original);in.add(new FrameNode(F_SAME, 0, null, 0, null));
                    m.instructions.insert(in);
                }
                if (m.name.equals("persist") && m.desc.equals(desc)) {
                    InsnList in = new InsnList();in.add(new VarInsnNode(ALOAD,1));in.add(new VarInsnNode(ALOAD,2));in.add(new VarInsnNode(ALOAD,3));
                    in.add(new MethodInsnNode(INVOKESTATIC, P + "RecoveryLog", "audit", "(L" + T + ";Ljava/lang/String;Ljava/util/UUID;)V", false));m.instructions.insert(in);
                }
                if (Set.of("deposit", "depositParent", "withdraw", "withdrawParent").contains(m.name)) startLog(m, m.name.toUpperCase(java.util.Locale.ROOT) + "_REQUEST", true, 2);
                if (m.name.equals("collect")) startLog(m, "COLLECT_REQUEST", true, null);
                if (m.name.equals("recoverSafe")) startLog(m, "ADMIN_RECOVER_REQUEST", false, null);
            }
        }

        if (c.name.equals(P + "ShirokaiDaycare")) {
            c.interfaces.add(P + "RecoveryHost");getter(c, "recoveryService", "service", "L" + S + ";");
            for (MethodNode m : c.methods) {
                if (m.name.equals("onInitializeServer")) {
                    InsnList in = new InsnList();in.add(new VarInsnNode(ALOAD,0));in.add(new FieldInsnNode(GETFIELD,c.name,"config","Ljava/nio/file/Path;"));
                    in.add(new MethodInsnNode(INVOKESTATIC,P+"RecoveryLog","install","(Ljava/nio/file/Path;)V",false));m.instructions.insert(in);
                }
                boolean admin = Arrays.stream(m.instructions.toArray()).anyMatch(n -> n instanceof LdcInsnNode d && "daycareadmin".equals(d.cst));
                if (admin) {
                    MethodInsnNode last = null;
                    for (AbstractInsnNode n : m.instructions) if (n instanceof MethodInsnNode call && call.owner.equals("com/mojang/brigadier/CommandDispatcher") && call.name.equals("register")) last = call;
                    if (last == null) throw new IllegalStateException("Admin registration not found");
                    InsnList in = new InsnList();in.add(new InsnNode(DUP));in.add(new VarInsnNode(ALOAD,0));
                    in.add(new MethodInsnNode(INVOKESTATIC,R,"register","(Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;L"+P+"RecoveryHost;)V",false));
                    m.instructions.insertBefore(last,in);registrations++;
                }
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(writer);return writer.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("input.jar compiled-classes-directory output.jar");
        String digest = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[0]))));
        if (!digest.equals("bcf361d485acfd9d21da05c213cfc0eda338414f77031bb3650a2dca103d3a18")) throw new IllegalArgumentException("Expected verified Zorykai server-pack 0.1.12 input");
        Set<String> targets = Set.of(S+".class",G+".class",T+".class",P+"storage/DaycareRepository.class",P+"ShirokaiDaycare.class",P+"config/Settings.class",P+"config/Settings$Messages.class",P+"ui/DaycareMenus.class");
        try (ZipFile src = new ZipFile(args[0]); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(Path.of(args[2])))) {
            for (ZipEntry e : Collections.list(src.entries())) {
                byte[] data = src.getInputStream(e).readAllBytes();
                if (targets.contains(e.getName())) data = patch(e.getName(), data);
                if (e.getName().equals("fabric.mod.json")) data = new String(data, StandardCharsets.UTF_8).replace("\"0.1.12-dev\"", "\"0.1.13-dev\"").getBytes(StandardCharsets.UTF_8);
                if (e.getName().equals("shirokai-daycare.mixins.json")) data = new String(data, StandardCharsets.UTF_8).replace("\"StoreReservationMixin\"", "\"StoreReservationMixin\",\n    \"PartyReservationMixin\",\n    \"PlayerPartyReservationMixin\",\n    \"PcReservationMixin\"").getBytes(StandardCharsets.UTF_8);
                ZipEntry copy = new ZipEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);out.write(data);out.closeEntry();
            }
            Path classes = Path.of(args[1]);
            try (var files = Files.walk(classes)) {
                for (Path file : files.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".class")).sorted().toList()) {
                    String name = classes.relativize(file).toString().replace('\\','/');
                    if (!name.startsWith(P)) throw new IllegalStateException("Unexpected compiled helper: " + name);
                    ZipEntry added = new ZipEntry(name);added.setTime(0);out.putNextEntry(added);out.write(Files.readAllBytes(file));out.closeEntry();
                }
            }
        }
        if (schemaBounds != 2 || schemaWrites != 3 || registrations != 1) throw new IllegalStateException("Patch expectations failed: bounds="+schemaBounds+", schema writes="+schemaWrites+", command="+registrations);
        System.out.println("Patched schema 4, PC-only outputs, reset journal and diagnostics");
    }
}
