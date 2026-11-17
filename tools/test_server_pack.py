"""JVM regression tests with explicit Minecraft protocol stubs, not a server test.

Usage: python3 tools/test_server_pack.py patched.jar gson.jar asm.jar asm-tree.jar
"""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

jar, gson, asm, tree = map(lambda p: str(Path(p).resolve()), sys.argv[1:])
sources = {
    'com/mojang/authlib/GameProfile.java': '''package com.mojang.authlib;
public record GameProfile(java.util.UUID id) { public java.util.UUID getId(){return id;} }''',
    'dev/shirokai/daycare/config/Settings.java': '''package dev.shirokai.daycare.config;
public class Settings { public General config = new General();
 public static class General { public boolean resourcePackEnabled; public String resourcePackUrl="",resourcePackSha1=""; } }''',
    'net/minecraft/class_2960.java': '''package net.minecraft;
public class class_2960 {public static class_2960 method_60655(String a,String b){return new class_2960();}}''',
    'net/minecraft/class_2856.java': '''package net.minecraft; import java.util.UUID;
public record class_2856(UUID comp_2163,class_2857 comp_2164) {
 public enum class_2857 {SUCCESSFULLY_LOADED,ACCEPTED,DOWNLOADED,FAILED_DOWNLOAD,DECLINED,DISCARDED,FAILED_RELOAD,INVALID_URL} }''',
    'net/minecraft/class_2596.java': 'package net.minecraft; public interface class_2596 {}',
    'net/minecraft/class_2720.java': '''package net.minecraft; import java.util.*;
public record class_2720(UUID id,String url,String hash,boolean required,Optional<class_2561> prompt) implements class_2596 {}''',
    'net/minecraft/class_3222.java': '''package net.minecraft; import java.util.UUID;
public class class_3222 {public final UUID id; public boolean host; public class_3244 field_13987=new class_3244();
 public class_3222(UUID id){this.id=id;} public UUID method_5667(){return id;} public boolean method_14239(){return host;} }''',
    'net/minecraft/class_3244.java': '''package net.minecraft;
public class class_3244 {public int sent; public class_2596 last; public void method_14364(class_2596 packet){sent++;last=packet;}}''',
    'net/minecraft/class_2561.java': '''package net.minecraft;
public interface class_2561 { public static class_5250 method_43470(String s){return new class_5250();}
 public static class_5250 method_43473(){return new class_5250();} public default class_5250 method_27661(){return new class_5250();} }''',
    'net/minecraft/class_5250.java': '''package net.minecraft;
public class class_5250 implements class_2561 {
 public class_5250 method_27694(java.util.function.UnaryOperator<class_2583> f){return this;}
 public class_5250 method_27692(class_124 c){return this;} public class_5250 method_10852(class_2561 t){return this;} }''',
    'net/minecraft/class_2583.java': '''package net.minecraft;
public class class_2583 {public class_2583 method_27704(class_2960 f){return this;} public class_2583 method_36139(int c){return this;}}''',
    'net/minecraft/class_124.java': 'package net.minecraft; public enum class_124 {field_1062,field_1068}',
    'net/minecraft/class_8609.java': '''package net.minecraft;
public class class_8609 {private final com.mojang.authlib.GameProfile profile;
 public class_8609(java.util.UUID id){profile=new com.mojang.authlib.GameProfile(id);}
 protected com.mojang.authlib.GameProfile method_52403(){return profile;}
 public void method_52395(class_2856 p){} }''',
    'net/minecraft/class_8610.java': '''package net.minecraft;
public class class_8610 extends class_8609 {public class_8610(java.util.UUID id){super(id);}}''',
    'Harness.java': '''import java.util.*;
import net.minecraft.*;
import dev.shirokai.daycare.ui.ResourcePackController;
import dev.shirokai.daycare.config.Settings;
public class Harness {
 static int checks;
 static UUID pack=UUID.fromString("cd77918b-6448-4bb4-8c7f-de4fda6a318a");
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message); checks++;}
 static void status(class_8609 handler, UUID id,class_2856.class_2857 status){handler.method_52395(new class_2856(id,status));}
 public static void main(String[] args){
  UUID id=UUID.randomUUID(); var p=new class_3222(id); var s=new Settings();
  var config=new class_8610(id);
  check(!ResourcePackController.loaded(p),"new configuration starts unloaded");
  status(config,UUID.randomUUID(),class_2856.class_2857.SUCCESSFULLY_LOADED);
  check(!ResourcePackController.loaded(p),"unrelated pack cannot activate UI");
  status(config,pack,class_2856.class_2857.ACCEPTED);
  check(!ResourcePackController.loaded(p),"accepted is not loaded");
  status(config,pack,class_2856.class_2857.DOWNLOADED);
  check(!ResourcePackController.loaded(p),"downloaded is not loaded");
  status(config,pack,class_2856.class_2857.SUCCESSFULLY_LOADED);
  check(ResourcePackController.loaded(p),"configuration-stage success activates UI");
  ResourcePackController.offer(p,s);
  check(p.field_13987.sent==0,"disabled mod sends no packet");
  check(ResourcePackController.loaded(p),"join preserves external confirmation");
  status(config,UUID.randomUUID(),class_2856.class_2857.FAILED_DOWNLOAD);
  check(ResourcePackController.loaded(p),"unrelated failure cannot clear UI");
  for(var failure:List.of(class_2856.class_2857.DECLINED,class_2856.class_2857.FAILED_DOWNLOAD,class_2856.class_2857.FAILED_RELOAD,class_2856.class_2857.DISCARDED,class_2856.class_2857.INVALID_URL)){
   status(config,pack,failure); check(!ResourcePackController.loaded(p),"failure disables UI: "+failure);
   status(config,pack,class_2856.class_2857.SUCCESSFULLY_LOADED);
  }
  new class_8610(id);
  check(!ResourcePackController.loaded(p),"new login/reconfiguration clears stale confirmation");
  ResourcePackController.offer(p,s);
  check(!ResourcePackController.loaded(p),"no pack confirmation means vanilla UI");
  status(config,pack,class_2856.class_2857.SUCCESSFULLY_LOADED);
  ResourcePackController.leave(id);
  check(!ResourcePackController.loaded(p),"disconnect clears confirmation");
  s.config.resourcePackEnabled=true;
  status(config,pack,class_2856.class_2857.SUCCESSFULLY_LOADED);
  ResourcePackController.offer(p,s);
  check(p.field_13987.sent==1,"original mod-managed mode still sends packet");
  check(!ResourcePackController.loaded(p),"mod-managed mode waits for its own confirmation");
  var packet=(class_2720)p.field_13987.last;
  check(packet.id().equals(pack),"original pack ID retained");
  check(packet.hash().equals(ResourcePackController.bundledSha1()),"bundled hash retained");
  s.config.resourcePackUrl="https://example.invalid/combined.zip";s.config.resourcePackSha1="a".repeat(40);
  ResourcePackController.offer(p,s);packet=(class_2720)p.field_13987.last;
  check(packet.url().equals(s.config.resourcePackUrl)&&packet.hash().equals(s.config.resourcePackSha1),"custom mod URL and SHA1 retained");
  p.host=true;ResourcePackController.offer(p,s);
  check(p.field_13987.sent==2,"host still receives no offer");
  var another=new class_3222(UUID.randomUUID());new class_8610(another.id);
  status(config,pack,class_2856.class_2857.SUCCESSFULLY_LOADED);
  check(!ResourcePackController.loaded(another),"confirmation isolated per player");
  System.out.println("PASS: "+checks+" JVM assertions (Minecraft protocol stubs; not a live server)");
 }
}''',
    'WeaveFixture.java': '''import java.nio.file.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class WeaveFixture implements Opcodes {
 public static void main(String[] args)throws Exception{
  try(var jar=new ZipFile(args[0])){
   weave(jar,Path.of(args[1]),"ResourcePackStatusMixin","net/minecraft/class_8609","shirokai$packStatus","method_52395");
   weave(jar,Path.of(args[1]),"ServerPackSessionMixin","net/minecraft/class_8610","zorykai$beginPackSession","<init>");
  }
 }
 static void weave(ZipFile jar,Path dir,String mixin,String target,String callback,String method)throws Exception{
  ClassNode source=new ClassNode();new ClassReader(jar.getInputStream(jar.getEntry("dev/shirokai/daycare/mixin/"+mixin+".class"))).accept(source,0);
  MethodNode hook=source.methods.stream().filter(m->m.name.equals(callback)).findFirst().orElseThrow();
  ClassNode node=new ClassNode();Path file=dir.resolve(target+".class");new ClassReader(Files.readAllBytes(file)).accept(node,0);
  MethodNode destination=node.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow();
  for(AbstractInsnNode i=destination.instructions.getFirst();i!=null;i=i.getNext())if(i.getOpcode()==RETURN){
   InsnList list=new InsnList();list.add(new VarInsnNode(ALOAD,0));
   // Inline exactly the generated callback, excluding unused CallbackInfo arguments.
   list.remove(list.getFirst());
   java.util.Map<LabelNode,LabelNode> labels=new java.util.HashMap<>();
   for(AbstractInsnNode n:hook.instructions)if(n instanceof LabelNode l)labels.put(l,new LabelNode());
   for(AbstractInsnNode n:hook.instructions)if(n.getOpcode()!=RETURN)list.add(n.clone(labels));
   destination.instructions.insertBefore(i,list);
  }
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(out);Files.write(file,out.toByteArray());
 }
}'''
}

with tempfile.TemporaryDirectory(prefix='zorykai-protocol-') as tmp:
    root = Path(tmp)
    for name, code in sources.items():
        f = root / name
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(code)
    cp = ':'.join([str(root), jar, gson, asm, tree])
    subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-cp', cp,
                    '-d', str(root), *map(str, root.rglob('*.java'))], check=True)
    subprocess.run(['java', '-cp', cp, 'WeaveFixture', jar, str(root)], check=True)
    subprocess.run(['java', '-Xverify:all', '-cp', cp, 'Harness'], check=True)

with zipfile.ZipFile(jar) as z:
    assert z.testzip() is None
    config = json.loads(z.read('shirokai-daycare.mixins.json'))
    assert config['mixins'].count('ServerPackSessionMixin') == 1
    print('PASS: archive CRCs and mixin registration')
