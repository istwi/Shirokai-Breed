"""Build a disposable test mod. Production metadata is never changed in the deliverable.
Usage: python3 tools/prepare_recovery_tests.py /workspace/zorykai-tools /workspace/recovery-runtime
Minecraft 1.21.1 and Fabric loader libraries must exist in /workspace/shirokai-runtime.
"""
from pathlib import Path
import hashlib
import json
import subprocess
import sys
import zipfile
import sqlite3
import uuid

repo = Path(__file__).resolve().parents[1]
tools, runtime = map(lambda p: Path(p).resolve(), sys.argv[1:])
runtime.mkdir(exist_ok=True)
mods = runtime / 'mods'
mods.mkdir(exist_ok=True)
classes = runtime / 'test-classes'
classes.mkdir(exist_ok=True)
candidate = tools / 'recovery-candidate.jar'
cp = [str(candidate), (tools / 'recovery-classpath.txt').read_text()]
subprocess.run(['java', '--add-modules', 'java.sql', '-m', 'jdk.compiler/com.sun.tools.javac.Main',
                '-proc:none', '-encoding', 'UTF-8', '-cp', ':'.join(cp), '-d', str(classes),
                *map(str, (repo / 'tests/recovery/java').rglob('*.java'))], check=True)

# API classes satisfy linking of unexecuted economy/permission branches. They are
# not providers. Payment and player LuckPerms behavior are explicitly outside this suite.
with zipfile.ZipFile(candidate) as source, zipfile.ZipFile(mods / 'daycare-validation-only.jar', 'w') as target:
    for name in source.namelist():
        data = source.read(name)
        if name == 'fabric.mod.json':
            meta = json.loads(data)
            meta['depends'].pop('impactor', None)
            meta['depends'].pop('luckperms', None)
            data = json.dumps(meta).encode()
        target.writestr(name, data)

seen = set()
with zipfile.ZipFile(mods / 'recovery-tests.jar', 'w') as target:
    for file in classes.rglob('*.class'):
        name = str(file.relative_to(classes));target.write(file, name);seen.add(name)
    def include(path, prefix):
        with zipfile.ZipFile(path) as source:
            for name in source.namelist():
                if name.startswith(prefix) and name.endswith('.class') and name not in seen:
                    target.writestr(name, source.read(name));seen.add(name)
    include(tools / 'impactor-5.3.5.jar', 'net/impactdev/impactor/api/')
    include(tools / 'luckperms-api.jar', 'net/luckperms/api/')
    with zipfile.ZipFile(tools / 'impactor-5.3.5.jar') as source:
        import io
        for name in source.namelist():
            if name.startswith('META-INF/jars/adventure-key-'):
                with zipfile.ZipFile(io.BytesIO(source.read(name))) as nested:
                    for n in nested.namelist():
                        if n.endswith('.class') and n not in seen: target.writestr(n, nested.read(n));seen.add(n)
    target.writestr('fabric.mod.json', json.dumps({'schemaVersion':1,'id':'zorykai_recovery_tests','version':'1.0.0',
        'environment':'server','entrypoints':{'server':['dev.shirokai.daycare.RecoveryLiveTest']}}))

import shutil
shutil.copy2(tools / 'cobblemon-1.8.3.jar', mods / 'cobblemon.jar')
shutil.copy2(tools / 'fabric-api.jar', mods / 'fabric-api.jar')
if not (runtime / 'server.properties').exists():
    (runtime / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25585\nonline-mode=false\nview-distance=2\nsimulation-distance=2\ngenerate-structures=false\nlevel-type=minecraft:flat\nmax-players=1\n')
# Acceptance is only for this user-requested isolated local testing runtime.
if not (runtime / 'eula.txt').exists():
    (runtime / 'eula.txt').write_text('eula=true\n')
(runtime / 'recovery-test-results.json').unlink(missing_ok=True)
# Seed a synthetic old-format record to verify schema-3 migration without using
# any uploaded player data. The game will create the rest of this disposable world.
dbpath=runtime/'world/shirokai-daycare/daycare.sqlite'
if not dbpath.exists():
    dbpath.parent.mkdir(parents=True,exist_ok=True)
    migration_id=str(uuid.UUID(bytes=hashlib.md5(b'zorykai-migration-schema3').digest(),version=3))
    state={'schema':3,'owner':migration_id,'revision':0,'originLanguage':'pt_br','manualEggCollection':True,
           'breedSlots':4,'incubatorSlots':3,'replacementCooldowns':{},'couples':[],'eggs':[]}
    payload=json.dumps(state,separators=(',',':'))
    with sqlite3.connect(dbpath) as db:
        db.execute('CREATE TABLE trainers(owner TEXT PRIMARY KEY,revision INTEGER NOT NULL,payload TEXT NOT NULL,sha256 TEXT NOT NULL)')
        db.execute('INSERT INTO trainers VALUES(?,?,?,?)',(migration_id,0,payload,hashlib.sha256(payload.encode()).hexdigest()))
print('Disposable runtime prepared:', runtime)
print('Never install daycare-validation-only.jar or recovery-tests.jar on a user server.')
