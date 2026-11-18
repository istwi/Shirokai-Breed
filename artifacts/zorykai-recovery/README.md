# Zorykai 0.1.13-dev — recuperação de vaga, PC e diagnóstico

Candidato de homologação baseado na variante Zorykai 0.1.12-dev com a correção
do pacote combinado. O pacote combinado publicado na branch é preservado.
Ainda não é um release oficial nem uma garantia de ausência de bugs.

## Instalação

Pare o servidor e faça um backup consistente do mundo inteiro, incluindo os
dados de equipe/PC do Cobblemon, o banco/journal do Daycare e as configurações.
Substitua o JAR antigo por `shirokai-daycare-0.1.13-dev-zorykai-recovery.jar` e
mantenha apenas um JAR Daycare em `mods/`. Preserve as configurações existentes,
inclusive as do pacote combinado em `server.properties`.

**O formato dos perfis passa para schema 4** para reconhecer o novo journal
`RESET_SLOT`. JARs anteriores não devem abrir esses dados. Para voltar a uma
versão antiga, restaure o backup completo com servidor parado, não apenas o JAR
ou apenas o SQLite: equipe/PC e custódia precisam corresponder ao mesmo momento.

## Comando de administrador

```mcfunction
/daycareadmin resetslot <jogador-online> <vaga-do-casal>
```

Exemplo para a primeira vaga de reprodução:

```mcfunction
/daycareadmin resetslot Fitiel 1
```

O número é **a vaga do casal no `/breed` (1–6), não o espaço da equipe**.
O jogador precisa estar online. A permissão é herdada de `/daycareadmin`:
operador nível 4 ou a permissão administrativa configurada via LuckPerms.

- Registra a intenção antes de mover qualquer Pokémon.
- Devolve ao **PC** os pais que ainda estão guardados, preservando os snapshots.
- Preserva um ovo pendente na chocadeira com UUID, genética e tempo restantes.
  Se a chocadeira estiver cheia, o reset é recusado; o ovo não é descartado nem
  chocado antecipadamente.
- Libera somente a vaga selecionada após confirmar a gravação do armazenamento.
- Preserva as vagas compradas e os demais casais/ovos.
- Não apaga operações pendentes, pagamentos ambíguos, snapshots divergentes ou
  UUIDs repetidos para forçar sucesso. Esses casos ficam bloqueados para análise.
- Se houver journal pendente ou perfil bloqueado, use primeiro
  `/daycareadmin recover <jogador>` e confira `/daycareadmin info <jogador>`.
  A solicitação de reset não significa que a confirmação assíncrona já terminou.

Uma vaga já vazia não é apagada novamente. O comando não limpa arbitrariamente
equipe/PC nem corrige uma identidade de login diferente. Use a capitalização
consistente do nick e o UUID correto.

## Todas as saídas vão ao PC

Retirada individual, retirada do casal, coleta do Pokémon incubado e devolução
do reset usam exclusivamente o PC. Espaços livres na equipe não substituem vagas
no PC. Se o PC estiver cheio, os Pokémon permanecem guardados ou a operação fica
pendente com seu journal preservado. A seleção para depósito continua aceitando
Pokémon da equipe ou do PC.

Os tooltips de destino e a mensagem padrão de capacidade foram atualizados.
A mensagem antiga conhecida de `full` é ajustada em memória; textos personalizados
e o prefixo do servidor não são sobrescritos.

O armazenamento é conferido pelos slots reais, além de verificar IDs e snapshots;
uma entrada obsoleta no índice por UUID não é aceita como confirmação. Reservas
são bloqueadas também nas camadas específicas de equipe/PC antes das notificações.
Uma remoção realmente recusada continua gerando erro e preservando a operação;
o código não força exclusão de dados para esconder a recusa.

## Logs locais

Criados em:

```text
config/shirokai-daycare/logs/daycare-AAAA-MM-DD.jsonl
```

Cada linha é um objeto JSON com hora UTC e ação. Quando disponíveis, inclui UUID
do jogador, vaga e UUID da operação. Registra depósitos, retiradas, coletas,
pedidos de `recover`/`resetslot`, etapas de journal, movimentações bloqueadas e
exceções do Daycare, inclusive falhas que chegam ao logger do servidor com stack
do mod. O log normal do servidor e a auditoria SQLite continuam disponíveis.
Erros que não são detectados pelo servidor, como um problema somente visual do
cliente, não podem ser identificados automaticamente apenas por esse histórico.

`config/shirokai-daycare/diagnostics.json` é criado sem substituir um arquivo
existente. Padrões:

```json
{
  "enabled": true,
  "maxFileBytes": 10485760,
  "retentionDays": 14
}
```

Reinicie para aplicar mudanças nesse arquivo. Há rotação por data e tamanho,
com até cinco segmentos anteriores por dia. A limpeza só remove arquivos com
nomes desse histórico; não apaga outros arquivos do administrador. Snapshots
completos de Pokémon e conteúdos dos inventários não são registrados nas ações.

## Validação e limitações

Veja `test-results.json`, `verification.json` e `SHA256SUMS` deste diretório.
Os testes usam Minecraft 1.21.1, Fabric Loader 0.17.2, Fabric API
0.116.6+1.21.1, Cobblemon 1.8.3, os stores reais e o SQLite do Daycare, com
fixtures descartáveis e sem jogadores conectados. Os corpos de classe testados
são os mesmos do JAR entregue. Somente a cópia de validação remove as exigências
de provedores Impactor/LuckPerms e disponibiliza suas APIs para linking.

Não foram testados pagamento com provedor, decisões reais de permissões de
jogadores no LuckPerms, login autenticado, múltiplos jogadores concorrentes,
renderização nem o incidente original com todos os mods do servidor. Não use os
JARs auxiliares de validação no seu servidor. Nenhum dado enviado do jogador foi
alterado ou incluído nos artefatos públicos.

O projeto Java original do mod não está neste repositório. A alteração é
reproduzível pelas fontes adicionais em `src/recovery/java` e pelo patch restrito
em `tools/PatchRecovery.java`; as entradas de regras de genética e economia
originais são mantidas.
