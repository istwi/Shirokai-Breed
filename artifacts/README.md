# Shirokai Daycare — candidato de homologação

Use `shirokai-daycare-0.1.1-dev.jar` (correção de gravação no Windows).
Pare o servidor, faça backup do mundo e dos dados do Daycare, remova o JAR
0.1.0 antigo de `mods/` e instale somente o novo JAR. Não instale ambos.

A versão 0.1.1 elimina a abertura de pastas via FileChannel no Windows, que
causava AccessDeniedException depois da confirmação do casal. Mantém flush do
arquivo e substituição atômica. O journal e os snapshots não são apagados nem
migrados. Depois de reiniciar e entrar, uma intenção de depósito pendente é
retomada. Se necessário, com o dono online:

```
/daycareadmin info <jogador>
/daycareadmin recover <jogador>
```

Não dê Pokémon substitutos e não apague o banco/journal para contornar o erro.
Este bloqueio de armazenamento não é uma falha de pagamento.

Servidor: Minecraft 1.21.1, Java 21, Fabric Loader 0.17.2, Fabric API
0.116.6+1.21.1, Cobblemon 1.8.3, Impactor Fabric 5.3.5 e LuckPerms Fabric 5.4.140.
Instalação exclusivamente no servidor. SQLite já está incluído.

33 testes automatizados passaram, incluindo a regressão do caminho Windows
simulado em Linux. Execução nativa em Windows ainda depende da homologação do
responsável. Não é release aprovado para produção; permanecem pendentes os
ensaios completos com clientes, rede, crashes reais e carga multiplayer.

SHA-256 em `SHA256SUMS`. A versão 0.1.0 permanece no histórico apenas como
candidato anterior, não deve ser usada para reproduzir a operação pendente.
