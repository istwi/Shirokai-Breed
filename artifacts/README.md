# Shirokai Daycare — candidato 0.1.3

## Downloads atuais

- [Mod do servidor 0.1.3](shirokai-daycare-0.1.3-dev.jar)
- [Pacote branco e ciano 0.1.3](shirokai-daycare-ui-0.1.3.zip)
- [Textura do menu principal](shirokai-ui-0.1.3-overview-texture.png)
- [Textura da tela do casal](shirokai-ui-0.1.3-couple-texture.png)
- `SHA256SUMS`: checksums dos artefatos.

As prévias mostram somente as texturas; não são capturas do Minecraft.

## O que mudou

Menus branco/ciano, sem figura lateral, com seis posições de casal e seis de
incubação sempre visíveis. Grades indicam espaços bloqueados; o tooltip informa
a ordem de compra, próximo espaço e preço. Reprodução e chocadeira têm navegação
central entre si, com ovo 3D original e ícone Pokémon do próprio Cobblemon.
A tela individual mostra os dois pais e o ovo central. Os tooltips explicam
incubação, coleta, capacidade, pagamento e pausa quando a chocadeira está cheia.

## Escolher qual ovo chocar

1. Abra `/breed` e clique em **ENTREGA: AUTOMÁTICA**, no rodapé.
2. Confirme **manual**. Cada casal passa a guardar no máximo um ovo pronto.
3. Abra o casal escolhido, clique no ovo central e confirme o envio à chocadeira.
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
2. Remova os JARs antigos do Daycare de `mods/` e instale **somente** o 0.1.3.
3. Reinicie, reconecte e aceite o novo pacote visual.
4. O banco antigo é lido e atualizado para guardar ovos nos casais. Após atualizar,
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

Passaram 44 testes Java, cinco testes Python e 85 verificações no servidor
dedicado. O teste manual usou dois casais, uma vaga e recarga real do SQLite.
Resultados: [validation-0.1.3.json](validation-0.1.3.json). Aparência final e transformação dos modelos precisam de conferência
no cliente autenticado. Este é um candidato de homologação; não foi criado
GitHub Release. Testes de carga multiplayer e crashes reais em todas as etapas
continuam pendentes.
