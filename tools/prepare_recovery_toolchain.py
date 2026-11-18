"""Prepare the verified toolchain used by build_recovery.py and the live tests.
Usage: python3 tools/prepare_recovery_toolchain.py /workspace/zorykai-tools /workspace/shirokai-runtime
Use dedicated build directories, not an existing game server.
"""
from pathlib import Path
import hashlib
import io
import json
import subprocess
import sys
import urllib.request
import zipfile

repo = Path(__file__).resolve().parents[1]
tools, runtime = map(lambda p: Path(p).resolve(), sys.argv[1:])
tools.mkdir(parents=True, exist_ok=True)
runtime.mkdir(parents=True, exist_ok=True)
base = repo / 'artifacts/zorykai-server-pack/shirokai-daycare-0.1.12-dev-zorykai-server-pack.jar'
if hashlib.sha256(base.read_bytes()).hexdigest() != 'bcf361d485acfd9d21da05c213cfc0eda338414f77031bb3650a2dca103d3a18':
    raise ValueError('Unexpected patch input')

def get(url):
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()

def artifact(name, url, expected=None):
    target = tools / name
    if expected is None:
        expected = get(url + '.sha1').decode().strip().split()[0]
        algorithm = 'sha1'
    else:
        algorithm = 'sha256'
    if not target.exists() or hashlib.new(algorithm, target.read_bytes()).hexdigest() != expected:
        data = get(url)
        if hashlib.new(algorithm, data).hexdigest() != expected:
            raise ValueError('Checksum mismatch: ' + name)
        target.write_bytes(data)
    print(name, algorithm, 'verified', flush=True)

artifact('asm.jar','https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar','8cadd43ac5eb6d09de05faecca38b917a040bb9139c7edeb4cc81c740b713281')
artifact('asm-tree.jar','https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar','9929881f59eb6b840e86d54570c77b59ce721d104e6dfd7a40978991c2d3b41f')
artifact('cobblemon-1.8.3.jar','https://maven.impactdev.net/repository/development/com/cobblemon/fabric/1.8.3+1.21.1/fabric-1.8.3+1.21.1.jar')
artifact('fabric-api.jar','https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.6+1.21.1/fabric-api-0.116.6+1.21.1.jar')
artifact('impactor-5.3.5.jar','https://maven.impactdev.net/repository/development/net/impactdev/impactor/launchers/fabric/5.3.5+1.21.1/fabric-5.3.5+1.21.1.jar','02312d1b101e67f9d2a0476ad6aafabb0b4cabab787f85ecf871ffce8d59f8a9')
artifact('luckperms-api.jar','https://repo.maven.apache.org/maven2/net/luckperms/api/5.4/api-5.4.jar')
subprocess.run(['python3',str(repo/'tools/prepare_fabric_probe.py'),str(runtime)],check=True)

# Obtain the intermediary game classes using Fabric's real remapper. The linkage
# probe exits before game startup and does not accept an EULA or create a world.
remapped = runtime / '.fabric/remappedJars/minecraft-1.21.1-0.17.2/server-intermediary.jar'
if not remapped.exists():
    mods = runtime / 'mods'
    if mods.exists() and any(p.name != 'zorykai-linkage-probe.jar' for p in mods.glob('*.jar')):
        raise ValueError('Runtime has other mods; use an isolated build directory')
    subprocess.run(['python3',str(repo/'tools/test_fabric_linkage.py'),str(base),str(runtime/'loader-libraries/fabric-loader.jar'),str(runtime)],check=True)
    with (runtime/'linkage-probe/toolchain-run.log').open('w') as log:
        subprocess.run(['java','-Xmx1G','-Xverify:all','-cp','loader-libraries/*:server.jar','net.fabricmc.loader.impl.launch.knot.KnotServer','nogui'],cwd=runtime,stdout=log,stderr=subprocess.STDOUT,check=True)
    if not (runtime/'linkage-probe/PASSED.txt').exists():raise ValueError('Linkage probe did not complete')

# Verify every bundled Minecraft library before putting it on the compiler path.
with zipfile.ZipFile(runtime/'server.jar') as server:
    for line in server.read('META-INF/libraries.list').decode().splitlines():
        digest, _, name = line.split('\t')
        data = server.read('META-INF/libraries/'+name)
        if hashlib.sha256(data).hexdigest()!=digest:raise ValueError('Bundled library mismatch: '+name)
        path = runtime/'libraries'/name;path.parent.mkdir(parents=True,exist_ok=True)
        if not path.exists():path.write_bytes(data)
        if hashlib.sha256(path.read_bytes()).hexdigest()!=digest:raise ValueError('Extracted library mismatch: '+name)

nested = tools/'cobblemon-libraries';nested.mkdir(exist_ok=True)
def extract(data):
    with zipfile.ZipFile(io.BytesIO(data)) as source:
        for name in source.namelist():
            if name.startswith('META-INF/jars/') and name.endswith('.jar'):
                value=source.read(name);(nested/Path(name).name).write_bytes(value);extract(value)
extract((tools/'cobblemon-1.8.3.jar').read_bytes())
extract((tools/'fabric-api.jar').read_bytes())
cp=[base,tools/'cobblemon-1.8.3.jar',remapped]
cp+=sorted((runtime/'libraries').rglob('*.jar'))+sorted((runtime/'loader-libraries').glob('*.jar'))+sorted(nested.glob('*.jar'))
(tools/'recovery-classpath.txt').write_text(':'.join(map(str,cp)))
manifest={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in tools.glob('*.jar') if p.name in ['asm.jar','asm-tree.jar','cobblemon-1.8.3.jar','fabric-api.jar','impactor-5.3.5.jar','luckperms-api.jar']}
(tools/'recovery-toolchain-sha256.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Toolchain ready:',tools)
