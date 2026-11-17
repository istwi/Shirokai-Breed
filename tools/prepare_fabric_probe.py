"""Download a checksum-verified minimal Minecraft/Fabric linkage-test runtime.

Usage: python3 tools/prepare_fabric_probe.py /workspace/shirokai-runtime
Does not run a server, accept the EULA, or install gameplay mods.
"""
import hashlib
import json
from pathlib import Path
import sys
import urllib.request

root = Path(sys.argv[1]).resolve()
libs = root / 'loader-libraries'
libs.mkdir(parents=True, exist_ok=True)

def fetch(url):
    with urllib.request.urlopen(url, timeout=90) as response:
        return response.read()

def download(url, path, expected, algorithm='sha1'):
    if path.is_file() and hashlib.new(algorithm, path.read_bytes()).hexdigest() == expected:
        return path.read_bytes()
    data = fetch(url)
    if hashlib.new(algorithm, data).hexdigest() != expected:
        raise ValueError(f'Checksum mismatch: {url}')
    path.write_bytes(data)
    return data

manifest = json.loads(fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
version = next(v for v in manifest['versions'] if v['id'] == '1.21.1')
metadata = json.loads(download(version['url'], root / 'minecraft-version.json', version['sha1']))
server = metadata['downloads']['server']
download(server['url'], root / 'server.jar', server['sha1'])

base = 'https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.17.2/fabric-loader-0.17.2'
expected = fetch(base + '.jar.sha1').decode().strip().split()[0]
download(base + '.jar', libs / 'fabric-loader.jar', expected)
profile = fetch(base + '.json')
(root / 'fabric-loader.json').write_bytes(profile)
for item in json.loads(profile)['libraries']['common']:
    group, artifact, version = item['name'].split(':')
    url = item['url'] + group.replace('.', '/') + '/' + artifact + '/' + version + '/' + artifact + '-' + version + '.jar'
    download(url, libs / (artifact + '.jar'), item['sha256'], 'sha256')
base = 'https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1-v2.jar'
expected = fetch(base + '.sha1').decode().strip().split()[0]
download(base, libs / 'intermediary.jar', expected)
print('Verified runtime prepared:', root)
