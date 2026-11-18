"""Rebuild recovery helpers against the existing Minecraft/Cobblemon test toolchain.
Usage: python3 tools/build_recovery.py /workspace/zorykai-tools
The directory must contain verified cobblemon-1.8.3.jar and recovery-classpath.txt.
"""
from pathlib import Path
import subprocess
import sys

repo = Path(__file__).resolve().parents[1]
tools = Path(sys.argv[1]).resolve()
classes = tools / 'recovery-classes'
classes.mkdir(exist_ok=True)
cp = (tools / 'recovery-classpath.txt').read_text()
subprocess.run(['java', '--add-modules', 'java.sql', '-m', 'jdk.compiler/com.sun.tools.javac.Main',
                '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes),
                *map(str, (repo / 'src/recovery/java').rglob('*.java'))], check=True)
subprocess.run(['java', '-cp', str(tools / 'asm.jar') + ':' + str(tools / 'asm-tree.jar'),
                str(repo / 'tools/PatchRecovery.java'),
                str(repo / 'artifacts/zorykai-server-pack/shirokai-daycare-0.1.12-dev-zorykai-server-pack.jar'),
                str(classes), str(tools / 'recovery-candidate.jar')], check=True)
