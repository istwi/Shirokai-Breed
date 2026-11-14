# Shirokai Daycare — candidato 0.1.5

## Downloads atuais

- [Mod do servidor 0.1.5](shirokai-daycare-0.1.5-dev.jar)
- [Pacote branco e ciano 0.1.5](shirokai-daycare-ui-0.1.5.zip)
- [Textura do menu principal](shirokai-ui-0.1.5-overview-texture.png)
- [Textura da tela do casal](shirokai-ui-0.1.5-couple-texture.png)
- `SHA256SUMS`: checksums dos artefatos.

As prévias mostram somente as texturas; não são capturas do Minecraft.

## O que mudou

0.1.5 remove os botões secundários marcados nas imagens e as telas de confirmação.
O clique executa a ação diretamente, incluindo compras pelo preço informado no
próprio tooltip. A tela do casal mantém os dois pais, ovo grande e um botão de
ovo no rodapé para abrir a chocadeira. Os menus principais mantêm apenas o botão
central de navegação. O ovo pronto da tela de incubação coleta ao clicar nele.

Clicar no modelo de um pai retira somente aquele Pokémon. O outro permanece
em custódia, com reprodução pausada. Clique no espaço vazio para selecionar um
parceiro compatível após o cooldown de troca. Retirar o último libera o casal.
Itens e atributos continuam preservados; ovos pendentes devem ser enviados antes
da retirada. Ações ainda verificam capacidade, permissão e operação pendente.

Ovo 3D branco/verde com dez camadas. Corrigida a referência da textura para a
pasta `textures/item/`, incluída no atlas de itens. As janelas dos modelos têm
fundo colorido para esconder as grades vanilla que apareciam atrás dos pais.

Menus branco/ciano, sem figura lateral, com seis posições de casal e seis de
incubação sempre visíveis. Grades indicam espaços bloqueados; o tooltip informa
a ordem de compra, próximo espaço e preço. Reprodução e chocadeira têm navegação
central entre si, com ovo 3D original e ícone Pokémon do próprio Cobblemon.
A tela individual mostra os dois pais e o ovo central. Os tooltips explicam
incubação, coleta, capacidade, pagamento e pausa quando a chocadeira está cheia.

## Escolher qual ovo chocar

1. Use `/breed entrega` para alternar entre manual e automático, sem confirmação.
   O chat informa o modo atual após a troca.
2. Em manual, cada casal guarda no máximo um ovo pronto.
3. Abra o casal escolhido e clique no ovo grande para enviá-lo imediatamente.
4. Sem vaga, o ovo continua no casal. Ele só incuba após o envio, enquanto o dono
   estiver online. A genética e o UUID já definidos não mudam com a escolha.
5. Colete o Pokémon pronto na chocadeira para liberar a vaga. Casais só voltam
   a reproduzir com espaço livre; um casal com ovo guardado espera o envio.

Para retirar os pais, envie primeiro o ovo guardado. Ao voltar ao automático,
ovos pendentes são enviados por ordem dos espaços assim que houver vaga.
Jogadores existentes continuam no modo automático até escolherem manual.
`manualEggCollectionDefault` define o padrão apenas de novos jogadores.

## Atualização segura

1. Pare o servidor e faça backup do mundo e dos dados do Daycare.
2. Remova os JARs antigos do Daycare de `mods/` e instale **somente** o 0.1.5.
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

O ovo 3D é uma criação visual original deste pacote, não um ovo nativo na equipe.
Pokémon usam a API pública PokemonItem e o renderizador do Cobblemon instalado.
Não há assets das referências, do Daycare+ ou do Cobbreeding. O pacote inclui dois
wrappers condicionais de modelos (`knowledge_book` e `cobblemon:pokemon_model`)
para CustomModelData 7301001/2 e 7301011. Servidores que já alteram esses mesmos
modelos devem unir os predicates ao combinar pacotes, preservando os demais.

## Validação

Runtime dedicado: Minecraft 1.21.1, Java 21, Fabric Loader 0.17.2,
Fabric API 0.116.6+1.21.1, Cobblemon 1.8.3, Impactor Fabric 5.3.5 e
LuckPerms Fabric 5.4.140. SQLite incluído no JAR.

Passaram 44 testes Java, seis testes Python e 94 verificações no servidor dedicado.
Resultados desta versão: [validation-0.1.5.json](validation-0.1.5.json).
O teste dedicado inclui retirada individual com falha injetada após gravação
externa, recarga SQLite, reposição do parceiro e retirada do último pai.
A aparência final depende de conferência em cliente autenticado. Este é um
candidato de homologação; não foi criado GitHub Release. Carga multiplayer,
login autenticado e crashes reais em todas as etapas continuam pendentes.
