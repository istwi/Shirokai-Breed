# Shirokai Daycare — candidato 0.1.2

## Arquivos atuais

- `shirokai-daycare-0.1.2-dev.jar`: mod para o servidor, com correção Windows e novos menus.
- `shirokai-daycare-ui-0.1.2.zip`: pacote de recursos original rosa/menta.
- `shirokai-daycare-ui-texture-preview.png`: prévia da textura, não captura do jogo.
- `SHA256SUMS`: checksums dos artefatos.

## Atualizar e recuperar o casal

1. Pare o servidor e faça backup do mundo/dados do Daycare.
2. Remova os JARs antigos 0.1.0/0.1.1 de `mods/` e instale **somente** o 0.1.2.
3. Inicie, conecte-se e aceite o pacote visual Shirokai quando solicitado.
4. Uma intenção de depósito pendente será retomada ao entrar. Se necessário,
   com o dono online, execute `/daycareadmin recover <jogador>` e confira
   `/daycareadmin info <jogador>`.

Não apague o banco/journal e não dê Pokémon substitutos para contornar a falha.
O erro Windows era a abertura de uma pasta via FileChannel, depois de salvar o
casal. O caminho novo força o arquivo renomeado sem abrir a pasta no Windows.
Flush do arquivo e substituição atômica continuam obrigatórios. A validação nativa
em Windows depende do teste do responsável; a regressão foi simulada em Linux.

## Pacote visual

O JAR oferece automaticamente o ZIP público deste repositório ao entrar, com
SHA-1 fixado. **Não coloque o ZIP em mods/**. Não é necessário outro mod cliente.
O pacote é opcional; o modo sem pacote usa inventário vanilla com cores similares.
Aceite o pacote e reabra `/breed` ou `/chocadeira` para aplicar o título visual.

O visual tem molduras rosa/menta, mascote de ovo próprio, cartões de espaços,
ícones Cobblemon dos pais e ovo central nas telas individuais. Não incorpora os
assets das imagens de referência. Não substitui arquivos `assets/minecraft/` de
outros menus/pacotes do servidor.

Configuração opcional em `config/shirokai-daycare/config.json`:

```json
{
  "resourcePackEnabled": true,
  "resourcePackUrl": "",
  "resourcePackSha1": ""
}
```

Adicione essas chaves ao arquivo existente sem remover as demais configurações.
URL vazia usa o pacote embutido nos metadados do JAR. Para hospedar outro ZIP,
use URL HTTPS pública e SHA-1 de 40 caracteres correspondentes. Depois reconecte.
Sem download ou sem aceitação, o Daycare mantém a interface vanilla.

## Compatibilidade e validação

Minecraft 1.21.1, Java 21, Fabric Loader 0.17.2, Fabric API 0.116.6+1.21.1,
Cobblemon 1.8.3, Impactor Fabric 5.3.5 e LuckPerms Fabric 5.4.140. Instalação do
mod exclusivamente no servidor; SQLite já incluído.

37 testes Java e 4 testes Python passaram. O servidor dedicado passou em 75
verificações de API/serviço, incluindo carregamento do bridge de status do pacote
e coordenadas vanilla dos inventários. Aparência dentro de cliente autenticado
não foi validada nesta sessão; a imagem de prévia representa somente a textura.

Continua sendo candidato de homologação. Não foi criado release público. Carga
multiplayer, rede real e crashes reais em todas as etapas continuam pendentes.
