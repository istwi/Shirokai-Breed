# Reprodução da correção do pacote Zorykai

Use Java 21 com `jdk.compiler` e Python 3. Os comandos abaixo partem de
`/workspace/Shirokai-Breed`; dependências e runtime de teste ficam fora do checkout.
Não alteram o ambiente publicado nem um servidor existente.

```sh
mkdir -p /workspace/zorykai-tools
curl -fL https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar -o /workspace/zorykai-tools/asm.jar
curl -fL https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar -o /workspace/zorykai-tools/asm-tree.jar
curl -fL https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar -o /workspace/zorykai-tools/gson.jar
sha256sum -c <<'SUMS'
8cadd43ac5eb6d09de05faecca38b917a040bb9139c7edeb4cc81c740b713281  /workspace/zorykai-tools/asm.jar
9929881f59eb6b840e86d54570c77b59ce721d104e6dfd7a40978991c2d3b41f  /workspace/zorykai-tools/asm-tree.jar
57928d6e5a6edeb2abd3770a8f95ba44dce45f3b23b7a9dc2b309c581552a78b  /workspace/zorykai-tools/gson.jar
SUMS
```

Prossiga apenas se os três checksums passarem. O patch recusa entradas diferentes
do SHA-256 da variante original 0.1.12-dev Zorykai. Para versões futuras, reveja
a implementação e os testes antes de adaptar a ferramenta.

```sh
java --class-path /workspace/zorykai-tools/asm.jar tools/PatchServerPack.java artifacts/zorykai-visual/shirokai-daycare-0.1.12-dev-zorykai-visual.jar /workspace/zorykai-tools/rebuilt.jar
python3 tools/test_server_pack.py /workspace/zorykai-tools/rebuilt.jar /workspace/zorykai-tools/gson.jar /workspace/zorykai-tools/asm.jar /workspace/zorykai-tools/asm-tree.jar
```

Resultado esperado: 23 asserções JVM passam, seguidas da verificação do arquivo
e do registro do mixin. Esse teste usa stubs, não um servidor.

Para verificar a aplicação real no Fabric, prepare um diretório **isolado**
sem outros mods ou dados de servidor:

```sh
python3 tools/prepare_fabric_probe.py /workspace/zorykai-tools/runtime
python3 tools/test_fabric_linkage.py /workspace/zorykai-tools/rebuilt.jar /workspace/zorykai-tools/runtime/loader-libraries/fabric-loader.jar /workspace/zorykai-tools/runtime
cd /workspace/zorykai-tools/runtime
java -Xmx1G -Xverify:all -cp 'loader-libraries/*:server.jar' net.fabricmc.loader.impl.launch.knot.KnotServer nogui > linkage-probe/run.log 2>&1
```

O comando Java deve terminar com código zero, escrever
`ZORYKAI_LINKAGE_PROBE_PASSED` no log atual e gerar `linkage-probe/PASSED.txt`.
A preparação do probe remove o marcador anterior. Esse processo usa classes reais
do Minecraft, aplica os mixins e exercita os callbacks sem iniciar o servidor,
aceitar EULA, abrir mundo, fazer login ou testar renderização. O probe usa
alocação sem construtor apenas no teste isolado para executar os callbacks; esse
código não é incluído no JAR entregue ao usuário.

O downloader verifica os hashes publicados por Mojang/Fabric e mantém TLS.
São necessários `piston-meta.mojang.com`, `piston-data.mojang.com` e
`maven.fabricmc.net`, além de Maven Central para as ferramentas. Não reduza
verificação se uma transferência falhar.
