# Reproduzir build e validação da recuperação

Requisitos: Java 21 com módulos `jdk.compiler`/`java.sql`, Python 3 e rede para os
domínios oficiais já usados pelo ambiente. Trabalhe no checkout existente; não
é necessário criar worktree. Use pastas de build/teste isoladas, sem dados de um
servidor real.

Na raiz do repositório:

```sh
python3 tools/prepare_recovery_toolchain.py /workspace/zorykai-tools /workspace/shirokai-runtime
python3 tools/build_recovery.py /workspace/zorykai-tools
python3 tools/run_recovery_tests.py /workspace/zorykai-tools /workspace/daycare-test-fresh /workspace/shirokai-runtime
```

O downloader verifica os hashes publicados e mantém TLS. O patch exige o
SHA-256 exato da variante Zorykai server-pack 0.1.12; não adapta automaticamente
uma versão diferente. O JAR candidato é escrito fora do checkout, em
`/workspace/zorykai-tools/recovery-candidate.jar`.

O runner prepara um servidor descartável em loopback, sem jogadores, com
fixtures de UUIDs próprios. Não rode em produção. Ele aceita a EULA somente
para esse teste local solicitado e cria um mundo separado. Recusa reutilizar
um mundo existente. O perfil sintético schema 3 verifica a migração, sem usar
nenhum anexo do jogador.

O resultado só é considerado aprovado se o processo Java terminar corretamente,
o arquivo de resultados pertencer à execução atual, todas as verificações tiverem
passado e o candidato não tiver mudado durante o teste. Não basta o servidor
abrir a porta ou o processo retornar zero. `recovery-test-results.json` registra
o hash do JAR testado e os resultados individuais.

Cobertura: seis espaços da equipe × IVs 0/15/31 × com/sem item; depósito,
retirada individual, reposição, retirada de ambos, snapshots completos e locks;
reset com/sem ovo, casal incompleto, chocadeira cheia, repetição de solicitação,
vaga não comprada, falha após gravação externa e retomada; coleta e retry
idempotente no PC; movimentações de reservas, PC cheio apesar de equipe livre;
pagamento pendente preservado; falha real da conexão SQLite, recarga canônica e
nova tentativa; comando registrado, acesso do console e bloqueio por nível;
formato antigo preservando vagas compradas; mensagens de capacidade e
preservação de texto personalizado.

O teste separado de diagnóstico confere registros JSON, jogador/operação/vaga,
captura de stack, rotação, retenção e preservação de arquivos alheios. Os erros
de SQLite e a exceção de diagnóstico são **injetados intencionalmente** e têm
asserções de segurança; não são ignorados para fazer o teste passar.

As fixtures acionam a lógica real de journal/armazenamento; não são cliques de
um cliente Minecraft. O mod de validação disponibiliza apenas APIs de
Impactor/LuckPerms para linking e remove essas duas exigências na cópia de
metadata de teste. Isso não valida serviços de economia ou permissões em
produção. Os dados originais desses módulos e do JAR entregue não são
substituídos por mocks. Nenhum JAR de validação deve ser distribuído como mod
de produção.
