"""Build an isolated pre-launch probe from the exact patched classes.

Usage: python3 tools/test_fabric_linkage.py patched.jar fabric-loader.jar runtime-directory
Run the generated probe with Fabric KnotServer; it exits BEFORE Minecraft startup.
This tests real Minecraft/Fabric linkage and injection, not gameplay or rendering.
"""
from pathlib import Path
import json
import subprocess
import sys
import zipfile

patched, loader, runtime = map(Path, sys.argv[1:])
runtime = runtime.resolve()
build = runtime / 'linkage-probe'
build.mkdir(parents=True, exist_ok=True)
(build / 'PASSED.txt').unlink(missing_ok=True)
source = build / 'PackLinkageProbe.java'
source.write_text('''import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
public class PackLinkageProbe implements PreLaunchEntrypoint {
 public void onPreLaunch() {
  try {
   var common=Class.forName("net.minecraft.class_8609");
   var config=Class.forName("net.minecraft.class_8610");
   if(Arrays.stream(common.getDeclaredMethods()).noneMatch(m->m.getName().contains("packStatus")))throw new AssertionError("Common-handler injection missing");
   if(Arrays.stream(config.getDeclaredMethods()).noneMatch(m->m.getName().contains("beginPackSession")))throw new AssertionError("Configuration-session injection missing");
   config.getDeclaredConstructors();
   var controller=Class.forName("dev.shirokai.daycare.ui.ResourcePackController");
   var packet=Class.forName("net.minecraft.class_2856");
   var action=Class.forName("net.minecraft.class_2856$class_2857");
   Object success=Arrays.stream(action.getEnumConstants()).filter(e->((Enum<?>)e).name().equals("SUCCESSFULLY_LOADED")).findFirst().orElseThrow();
   var owner=UUID.randomUUID(); var id=UUID.fromString("cd77918b-6448-4bb4-8c7f-de4fda6a318a");
   Object response=packet.getConstructor(UUID.class,action).newInstance(id,success);
   controller.getMethod("status",UUID.class,packet).invoke(null,owner,response);
   var trackerField=controller.getDeclaredField("TRACKER");trackerField.setAccessible(true);Object tracker=trackerField.get(null);
   Method loaded=tracker.getClass().getMethod("loaded",UUID.class);
   if(!(Boolean)loaded.invoke(tracker,owner))throw new AssertionError("Real protocol success not tracked");
   // Isolate the actual injected callbacks without starting a world or sockets.
   var unsafeClass=Class.forName("sun.misc.Unsafe");
   var unsafeField=unsafeClass.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
   Object handler=unsafeClass.getMethod("allocateInstance",Class.class).invoke(unsafeField.get(null),config);
   var profile=Class.forName("com.mojang.authlib.GameProfile").getConstructor(UUID.class,String.class).newInstance(owner,"PackProbe");
   var profileField=config.getDeclaredField("field_45022");profileField.setAccessible(true);profileField.set(handler,profile);
   Method begin=Arrays.stream(config.getDeclaredMethods()).filter(m->m.getName().contains("beginPackSession")).findFirst().orElseThrow();begin.setAccessible(true);
   Method callback=Arrays.stream(common.getDeclaredMethods()).filter(m->m.getName().contains("packStatus")).findFirst().orElseThrow();callback.setAccessible(true);
   begin.invoke(handler,new Object[]{null});
   if((Boolean)loaded.invoke(tracker,owner))throw new AssertionError("Actual injected session reset failed");
   callback.invoke(handler,response,null);
   if(!(Boolean)loaded.invoke(tracker,owner))throw new AssertionError("Actual configuration-handler status injection failed");
   begin.invoke(handler,new Object[]{null});
   if((Boolean)loaded.invoke(tracker,owner))throw new AssertionError("Actual reconnect reset failed");
   controller.getMethod("leave",UUID.class).invoke(null,owner);
   if((Boolean)loaded.invoke(tracker,owner))throw new AssertionError("Cleanup failed");
   Files.writeString(Path.of("linkage-probe/PASSED.txt"),"PASS: real Minecraft 1.21.1 handlers transformed by Fabric 0.17.2 / Mixin; actual injected callbacks, protocol status, reconnect reset and cleanup verified. No server/gameplay/client test.\\n");
   System.out.println("ZORYKAI_LINKAGE_PROBE_PASSED");
   System.exit(0);
  } catch(Throwable t) {t.printStackTrace();System.exit(1);}
 }
}
''')
subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-cp', str(loader.resolve()),
                '-d', str(build), str(source)], check=True)
mods = runtime / 'mods'
mods.mkdir(exist_ok=True)
with zipfile.ZipFile(patched) as src, zipfile.ZipFile(mods / 'zorykai-linkage-probe.jar', 'w') as dest:
    for name in src.namelist():
        if (name.startswith('dev/shirokai/daycare/config/Settings') or
            name in ['dev/shirokai/daycare/ui/ResourcePackController.class',
                     'dev/shirokai/daycare/ui/PackStatusTracker.class',
                     'dev/shirokai/daycare/mixin/ResourcePackStatusMixin.class',
                     'dev/shirokai/daycare/mixin/ServerPackSessionMixin.class',
                     'shirokai-daycare-refmap.json', 'shirokai-ui-pack.json']):
            dest.writestr(name, src.read(name))
    dest.writestr('PackLinkageProbe.class', (build / 'PackLinkageProbe.class').read_bytes())
    dest.writestr('fabric.mod.json', json.dumps({
        'schemaVersion': 1, 'id': 'zorykai_linkage_probe', 'version': '1.0.0',
        'environment': 'server', 'entrypoints': {'preLaunch': ['PackLinkageProbe']},
        'mixins': ['zorykai-probe.mixins.json']}))
    dest.writestr('zorykai-probe.mixins.json', json.dumps({
        'required': True, 'package': 'dev.shirokai.daycare.mixin',
        'compatibilityLevel': 'JAVA_21',
        'mixins': ['ResourcePackStatusMixin', 'ServerPackSessionMixin'],
        'injectors': {'defaultRequire': 1}, 'refmap': 'shirokai-daycare-refmap.json'}))
print('Built validation-only probe (never install this probe in a user server):', mods / 'zorykai-linkage-probe.jar')
