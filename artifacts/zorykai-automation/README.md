# Zorykai Daycare 0.1.14-dev — ovos automáticos, fila e VIP

Base: branch `zorykai`, commit `ba523ae` (0.1.13-dev, recuperação de vaga/PC). A branch `main` consultada está em 0.1.12-dev. Foram verificadas todas as branches remotas existentes (`main` e `zorykai`). Minecraft 1.21.1, Java 21 e Cobblemon 1.8.3; manter Impactor e LuckPerms já utilizados.

## Instalação

1. Com o servidor parado, fazer backup completo e consistente do mundo, dados de equipe/PC, SQLite/journal do Daycare e configurações.
2. Substituir o JAR antigo por `shirokai-daycare-0.1.14-dev-zorykai-automation.jar`. Manter somente um JAR Daycare em `mods/`.
3. Preservar `config/shirokai-daycare`, o banco SQLite e o pacote de recursos Zorykai atual. Não apagar os Pokémon, casais, ovos nem as vagas compradas.
4. Testar primeiro em homologação. Não foram aplicadas mudanças no servidor de produção.

**O formato dos perfis passa para schema 5**, com migração dos perfis anteriores para preservar bases de tempo e a ordem da fila. JARs antigos não devem abrir esses dados. Para voltar à versão anterior, restaurar o backup completo com servidor parado; não voltar apenas o JAR ou apenas o SQLite.

## Funcionamento

- Ao concluir a reprodução, o ovo entra automaticamente na chocadeira se houver vaga.
- Com a chocadeira cheia, os casais continuam o ciclo até produzirem **um ovo por casal**. O ovo fica guardado em uma fila persistente; o casal espera sem produzir outro.
- A fila segue a ordem de chegada. Ao coletar um filhote e confirmar a entrega no PC, o ovo mais antigo ocupa a vaga automaticamente na próxima atualização do sistema, normalmente em até um segundo. O casal que liberou o ovo inicia o próximo ciclo.
- O ovo em espera não incuba na fila. Seu tempo de incubação começa ao entrar na chocadeira.
- Ovos já prontos continuam ocupando a chocadeira até a coleta. Se o PC estiver cheio, o filhote fica guardado e a vaga não é liberada indevidamente.
- A fila, UUID, genética, pais, tempo e ordem são preservados no SQLite. Se existir operação pendente ou falha de persistência, a automação aguarda a recuperação; não apaga o journal para avançar.
- A regra anterior de tempo apenas com o jogador online permanece. A pausa administrativa também é respeitada.

A coleta e a retirada de pais mantêm a mesma janela aberta, com atualização dos itens após a confirmação da operação. A compra usa o mesmo caminho sem fechamento automático. Ao depositar um parceiro escolhido, a interface retorna à tela do casal após confirmar o depósito. Navegar entre telas e fechar voluntariamente continuam funcionando. Cliques repetidos não dispensam as verificações de estado/journal.

As telas mostram o envio automático e a fila. O jogador não precisa clicar para transferir um ovo; `/breed entrega` informa o modo automático. O campo legado de coleta manual deixa de controlar o envio. Corrigida também a identificação física da primeira vaga da chocadeira (índice zero); ovos antigos em índices inconsistentes são reposicionados sem recriar o Pokémon.

## VIP: 20 minutos viram 15 minutos

O arquivo é criado automaticamente:

`config/shirokai-daycare/time-bonuses.json`

```json
{
  "schema": 1,
  "rules": [
    {
      "permission": "shirokai.daycare.vip",
      "breedingReductionPercent": 25.0,
      "incubationReductionPercent": 25.0
    }
  ]
}
```

Na criação desse arquivo, a permissão acompanha `vipPermission` da configuração existente; acima está o padrão do mod. Para conceder ao grupo VIP:

```mcfunction
/lp group vip permission set shirokai.daycare.vip true
```

Trocar `vip` pelo nome real do grupo do servidor. Não depende de OP. A consulta usa o LuckPerms e o contexto real do jogador. Uma negação explícita não concede o bônus.

A redução é proporcional: **25% a menos** na reprodução e na incubação. Um tempo base de 20 minutos passa a 15; um de 40 passa a 30. A atualização não troca seus tempos base por 20 minutos nem altera os preços/descontos de compra de vagas.

Os percentuais são independentes, de 0 a 99. É possível adicionar outras permissões à lista; prevalece o maior desconto de cada categoria, sem somar reduções. Uma lista vazia desativa os bônus de tempo. Alterações podem ser carregadas com `/daycareadmin reload` ou reinício.

Ganhar/perder VIP ou recarregar os percentuais preserva a porcentagem já concluída do ciclo. As bases ficam salvas para o desconto não se reaplicar a cada segundo nem a cada reinício. Um ovo já concluído não volta a incubar por perder VIP.

## Validação realizada

- 71 verificações JVM de timers, limites, fila, ordem, capacidade 1–6, permissões, proporcionalidade, não acumulação, JSON e migração.
- 669 verificações da suíte de recuperação anterior, com o schema novo: custódia, snapshots, PC, journal, cliques/requisições repetidas, falhas controladas e recuperação.
- Cliente TCP automatizado em servidor local com o JAR de produção e os mods reais Cobblemon, LuckPerms e Impactor: reprodução com chocadeira cheia, fila, 20→15 minutos, negação do VIP, coleta, preenchimento automático, repetição de cliques, retirada e depósito de pais.
- Segundo boot do mesmo mundo: fila, UUIDs, snapshots, ordem e tempos VIP preservados; coleta mantém a janela e libera a vaga para o ovo mais antigo, sem duplicação.
- Na coleta e na retirada, o teste confirmou a mesma janela e ausência de pacote de fechamento ou reabertura.

O cliente automatizado consome os pacotes de apresentação de receitas, itens e metadados do Cobblemon como bytes, pois o decodificador vanilla não conhece os tipos adicionais. As etiquetas e revisões são lidas dos itens reais do servidor pela fixture de console; os cliques, abertura/fechamento e conexão usam TCP. Isso não testa a renderização visual do pacote de recursos em um cliente humano. Não houve alteração em produção, teste humano ou novo ensaio de pagamentos de compra de vagas.

As fixtures de teste não estão no JAR entregue. O pacote de recursos, arquivos não executáveis e SQLite embarcado da versão anterior foram preservados. Os resultados resumidos e o SHA-256 acompanham o artefato.

## Fontes e reprodução

A branch anterior distribui parte do mod apenas em binário. Na cópia de trabalho e no pacote de fontes local desta entrega, as fontes reconstruídas dos modelos/menus e as novas classes estão em `src/automation/java`; o patch versionado está em `tools/PatchAutomation.java`. Os arquivos deste diretório no GitHub são artefatos de distribuição; as alterações do código-fonte não foram publicadas na árvore do repositório. Ele exige o SHA-256 exato do JAR 0.1.13 e valida os pontos alterados, preservando as demais entradas.

Com Java 21 no PATH:

```sh
python3 tools/prepare_automation_toolchain.py /workspace/breed-tools /workspace/breed-linkage-runtime
python3 tools/build_automation.py /workspace/breed-tools
```

Usar diretórios isolados. Os scripts de teste TCP desta entrega usam o template local Fabric e módulos Node do workspace de validação; não apontá-los para um servidor real. Não instalar `automation-fixture.jar` nem o mod de recuperação de testes em produção.
