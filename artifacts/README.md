# Shirokai Daycare — candidato 0.1.7

## Downloads atuais

- [Mod do servidor 0.1.7](shirokai-daycare-0.1.7-dev.jar)
- [Pacote branco e ciano 0.1.7](shirokai-daycare-ui-0.1.7.zip)
- [Textura do menu principal](shirokai-ui-0.1.7-overview-texture.png)
- [Textura da tela do casal](shirokai-ui-0.1.7-couple-texture.png)
- [Textura da incubadora](shirokai-ui-0.1.7-incubation-texture.png)
- [Textura do ovo Shirokai](../resourcepack/assets/shirokai_daycare/textures/item/egg_dragon_shirokai.png)
- `SHA256SUMS`: checksums dos artefatos.

As prévias mostram somente as texturas; não são capturas do Minecraft.

## O que mudou

0.1.7 usa a geometria 3D do ovo do Ender Dragon já presente no cliente,
referenciada por `minecraft:block/dragon_egg`, com textura original branca/verde,
sombras suaves em pixel art e pequenos brilhos ciano. A geometria e a textura
vanilla não são redistribuídas; o modelo do Shirokai apenas herda a geometria e
substitui a textura `all`. A alteração não muda o ovo do Ender Dragon no mundo.

Removido o tempo para trocar os pais. A retirada individual e a retirada do
casal não criam mais cooldown; tempos antigos salvos são descartados ao carregar
o jogador. `replacementCooldownSeconds` antigo é ignorado para a troca.

A tela individual da incubadora mostra só o ovo central e o botão de navegação,
sem os pais nem molduras laterais. IDs de ovos foram removidos dos tooltips.

A entrega é **sempre manual**, inclusive para jogadores antigos. Ovos novos
ficam no casal até o clique no ovo grande; não entram automaticamente quando
uma vaga é liberada. Ovos que já estavam na chocadeira permanecem onde estão,
com os tempos e atributos preservados. Os UUIDs internos continuam no banco e
no journal para impedir duplicações, sem aparecer nos menus do jogador.

Não há telas de confirmação. O clique executa a ação diretamente, incluindo
compras pelo preço informado no tooltip. Menus principais mantêm apenas o botão
central de navegação; o ovo pronto da incubadora coleta ao clicar nele.

Clicar no modelo de um pai retira somente aquele Pokémon. O outro permanece
em custódia, com reprodução pausada. Clique no espaço vazio para selecionar um
parceiro compatível imediatamente. Retirar o último libera o casal.
Itens e atributos continuam preservados; ovos pendentes devem ser enviados antes
da retirada. Ações ainda verificam capacidade, permissão e operação pendente.

Menus branco/ciano, sem figura lateral, com seis posições de casal e seis de
incubação sempre visíveis. Grades indicam espaços bloqueados; o tooltip informa
a ordem de compra, próximo espaço e preço. Reprodução e chocadeira têm navegação
central entre si, com ovo 3D usando o modelo vanilla e ícone Pokémon do próprio Cobblemon.
A tela individual mostra os dois pais e o ovo central. Os tooltips explicam
incubação, coleta, capacidade, pagamento e pausa quando a chocadeira está cheia.

## Escolher qual ovo chocar

1. Espere o casal terminar a reprodução: o ovo fica guardado nele.
2. Abra o casal escolhido e clique no ovo grande para enviá-lo à chocadeira.
3. Sem vaga, ele fica no casal. Só incuba após o envio e enquanto o dono estiver
   online. A genética e o UUID já definidos não mudam com a escolha.
4. Clique no ovo pronto na incubadora para coletar o Pokémon e liberar a vaga.

Cada casal guarda no máximo um ovo; envie-o antes de retirar qualquer pai.
Não existe mais modo automático. `/breed entrega` informa como enviar manualmente.
Configurações antigas de entrega automática são ignoradas. Chocadeira cheia
continua pausando a reprodução, sem descartar ovos guardados.

## Atualização segura

1. Pare o servidor e faça backup do mundo e dos dados do Daycare.
2. Remova os JARs antigos do Daycare de `mods/` e instale **somente** o 0.1.7.
3. Reinicie, reconecte e aceite o novo pacote visual.
4. O banco antigo é atualizado para formato 3, que permite um pai aguardando parceiro. Após atualizar,
   **não volte a um JAR antigo com o banco novo**. Para reverter, restaure também
   o backup anterior completo, com servidor parado.

Não apague banco/journal nem entregue Pokémon substitutos para contornar uma
operação pendente. A correção Windows de 0.1.1 continua incluída: força o arquivo
renomeado sem tentar abrir diretórios via FileChannel. Flush do arquivo e troca
atômica continuam obrigatórios. Regressão simulada em Linux; teste nativo Windows
não foi realizado nesta sessão.

Um depósito interrompido é retomado ao entrar; se necessário, com o dono online,
use `/daycareadmin recover <jogador>` e confira `/daycareadmin info <jogador>`.
Pagamento sem comprovante definitivo exige conferência do administrador antes de
marcar pago/não pago. A nova UI não resolve automaticamente um débito ambíguo.

## Instalar o visual

O JAR oferece o ZIP público deste repositório com SHA-1 fixado. **Não coloque o
ZIP em mods/**. Não exige mod cliente adicional ao Cobblemon. Sem aceitação,
o menu funciona com aparência vanilla branco/ciano.

Em `config/shirokai-daycare/config.json`, `resourcePackEnabled: true` e
`resourcePackUrl: ""` usam os metadados atuais do JAR. Se foi configurada uma URL
antiga, substitua URL e SHA-1 juntos ou deixe a URL vazia. Remova/desative o pacote
Shirokai antigo instalado manualmente e reconecte para aceitar a nova versão.

A textura do ovo é original; a geometria vem do cliente Minecraft. Não é um ovo nativo na equipe.
Pokémon usam a API pública PokemonItem e o renderizador do Cobblemon instalado.
Não há assets das referências, do Daycare+ ou do Cobbreeding. O pacote inclui dois
wrappers condicionais de modelos (`knowledge_book` e `cobblemon:pokemon_model`)
para CustomModelData 7301001/2 e 7301011. Servidores que já alteram esses mesmos
modelos devem unir os predicates ao combinar pacotes, preservando os demais.

## Validação

Runtime dedicado: Minecraft 1.21.1, Java 21, Fabric Loader 0.17.2,
Fabric API 0.116.6+1.21.1, Cobblemon 1.8.3, Impactor Fabric 5.3.5 e
LuckPerms Fabric 5.4.140. SQLite incluído no JAR.

0.1.7: compilação, 44 testes Java e sete testes Python. As 97 verificações no
servidor dedicado foram executadas na 0.1.6; não foram repetidas nesta versão.
Resultados desta versão: [validation-0.1.7.json](validation-0.1.7.json).
O teste dedicado verifica que uma configuração antiga de envio automático não
envia ovos sem clique, além de retirada individual com falha injetada após gravação
externa, recarga SQLite, reposição do parceiro e retirada do último pai.
A aparência final depende de conferência em cliente autenticado. Este é um
candidato de homologação; não foi criado GitHub Release. Carga multiplayer,
login autenticado e crashes reais em todas as etapas continuam pendentes.
