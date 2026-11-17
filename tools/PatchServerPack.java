import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Narrow binary patch: this distribution repository does not contain mod sources. */
public class PatchServerPack implements Opcodes {
    static final String CONTROLLER = "dev/shirokai/daycare/ui/ResourcePackController";
    static final String TRACKER = "dev/shirokai/daycare/ui/PackStatusTracker";
    static final String COMMON = "net/minecraft/class_8609";
    static final String CONFIG = "net/minecraft/class_8610";
    static final String PACKET = "net/minecraft/class_2856";
    static final String MIXIN = "dev/shirokai/daycare/mixin/ServerPackSessionMixin";
    static void playerId(MethodVisitor m) {
        m.visitVarInsn(ALOAD, 0);
        m.visitTypeInsn(CHECKCAST, COMMON);
        m.visitMethodInsn(INVOKEVIRTUAL, COMMON, "method_52403", "()Lcom/mojang/authlib/GameProfile;", false);
        m.visitMethodInsn(INVOKEVIRTUAL, "com/mojang/authlib/GameProfile", "getId", "()Ljava/util/UUID;", false);
    }
    static byte[] controller(byte[] original) {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(original).accept(new ClassVisitor(ASM9, w) {
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor target = super.visitMethod(access, name, desc, sig, ex);
                if (!name.equals("offer")) return target;
                return new MethodVisitor(ASM9, target) {
                    public void visitCode() {
                        super.visitCode();
                        Label enabled = new Label();
                        visitVarInsn(ALOAD, 1);
                        visitFieldInsn(GETFIELD, "dev/shirokai/daycare/config/Settings", "config", "Ldev/shirokai/daycare/config/Settings$General;");
                        visitFieldInsn(GETFIELD, "dev/shirokai/daycare/config/Settings$General", "resourcePackEnabled", "Z");
                        visitJumpInsn(IFNE, enabled);
                        visitInsn(RETURN);
                        visitLabel(enabled);
                        visitFrame(F_SAME, 0, null, 0, null);
                    }
                };
            }
            public void visitEnd() {
                MethodVisitor m = super.visitMethod(ACC_PUBLIC | ACC_STATIC, "status", "(Ljava/util/UUID;L" + PACKET + ";)V", null, null);
                m.visitCode();
                m.visitFieldInsn(GETSTATIC, CONTROLLER, "TRACKER", "L" + TRACKER + ";");
                m.visitVarInsn(ALOAD, 0);
                m.visitVarInsn(ALOAD, 1);
                m.visitMethodInsn(INVOKEVIRTUAL, PACKET, "comp_2163", "()Ljava/util/UUID;", false);
                m.visitVarInsn(ALOAD, 1);
                m.visitMethodInsn(INVOKEVIRTUAL, PACKET, "comp_2164", "()L" + PACKET + "$class_2857;", false);
                m.visitMethodInsn(INVOKEVIRTUAL, PACKET + "$class_2857", "name", "()Ljava/lang/String;", false);
                m.visitMethodInsn(INVOKEVIRTUAL, TRACKER, "status", "(Ljava/util/UUID;Ljava/util/UUID;Ljava/lang/String;)V", false);
                m.visitInsn(RETURN);
                m.visitMaxs(0, 0);
                m.visitEnd();
                super.visitEnd();
            }
        }, 0);
        return w.toByteArray();
    }
    static byte[] statusMixin(byte[] original) {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(original).accept(new ClassVisitor(ASM9, w) {
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor target = super.visitMethod(access, name, desc, sig, ex);
                if (!name.equals("shirokai$packStatus")) return target;
                // Retain injection annotations, replace only the method body.
                return new MethodVisitor(ASM9) {
                    public AnnotationVisitor visitAnnotation(String d, boolean v) { return target.visitAnnotation(d, v); }
                    public void visitEnd() {
                        target.visitCode();
                        playerId(target);
                        target.visitVarInsn(ALOAD, 1);
                        target.visitMethodInsn(INVOKESTATIC, CONTROLLER, "status", "(Ljava/util/UUID;L" + PACKET + ";)V", false);
                        target.visitInsn(RETURN);
                        target.visitMaxs(0, 0);
                        target.visitEnd();
                    }
                };
            }
        }, 0);
        return w.toByteArray();
    }
    static byte[] sessionMixin() {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        w.visit(V21, ACC_PUBLIC | ACC_ABSTRACT, MIXIN, null, "java/lang/Object", null);
        AnnotationVisitor a = w.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor values = a.visitArray("value");
        values.visit(null, Type.getObjectType(CONFIG)); values.visitEnd();
        a.visitEnd();
        MethodVisitor m = w.visitMethod(ACC_PRIVATE, "zorykai$beginPackSession", "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", null, null);
        a = m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;", true);
        values = a.visitArray("method"); values.visit(null, "<init>"); values.visitEnd();
        values = a.visitArray("at");
        AnnotationVisitor at = values.visitAnnotation(null, "Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value", "RETURN"); at.visitEnd(); values.visitEnd(); a.visitEnd();
        m.visitCode(); playerId(m);
        m.visitMethodInsn(INVOKESTATIC, CONTROLLER, "leave", "(Ljava/util/UUID;)V", false);
        m.visitInsn(RETURN); m.visitMaxs(0, 0); m.visitEnd();
        w.visitEnd(); return w.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input.jar output.jar");
        String digest = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[0]))));
        if (!digest.equals("2ba8acdda28047b8e2f1201ffe648a7b1f798c35904bdb668955f3049fa8e8f6")) {
            throw new IllegalArgumentException("Expected the verified latest 0.1.12-dev Zorykai visual input; inspect newer versions before adapting this patch");
        }
        if (Path.of(args[0]).toRealPath().equals(Path.of(args[1]).toAbsolutePath().normalize())) throw new IllegalArgumentException("Do not overwrite input");
        try (ZipFile input = new ZipFile(args[0]); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            for (ZipEntry entry : Collections.list(input.entries())) {
                byte[] bytes = input.getInputStream(entry).readAllBytes();
                switch (entry.getName()) {
                    case "dev/shirokai/daycare/ui/ResourcePackController.class" -> bytes = controller(bytes);
                    case "dev/shirokai/daycare/mixin/ResourcePackStatusMixin.class" -> bytes = statusMixin(bytes);
                    case "shirokai-daycare.mixins.json" -> {
                        String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                        if (!json.contains("\"ResourcePackStatusMixin\"")) throw new IllegalStateException("Unexpected mixin configuration");
                        bytes = json.replace("\"ResourcePackStatusMixin\"", "\"ResourcePackStatusMixin\",\n    \"ServerPackSessionMixin\"").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
                ZipEntry dest = new ZipEntry(entry.getName()); dest.setTime(entry.getTime());
                out.putNextEntry(dest); out.write(bytes); out.closeEntry();
            }
            ZipEntry added = new ZipEntry(MIXIN + ".class"); added.setTime(0);
            out.putNextEntry(added); out.write(sessionMixin()); out.closeEntry();
        }
    }
}
