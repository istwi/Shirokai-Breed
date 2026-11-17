# Zorykai — alteração somente visual do JAR

Base conferida em todas as branches remotas de `istwi/Shirokai-Breed`:
**somente `main`**, commit `34df976fcfc495ae938eee08a29b8ffc9bd66ac2`.
O maior número de versão entre os JARs dessa branch é **0.1.12-dev**.
O arquivo original foi extraído diretamente desse commit, sem usar o build local.

## Instalar

Pare o servidor e substitua o JAR antigo por
[shirokai-daycare-0.1.12-dev-zorykai-visual.jar](shirokai-daycare-0.1.12-dev-zorykai-visual.jar).
O ID interno e a versão continuam `shirokai_daycare` / `0.1.12-dev`;
instale apenas um JAR do Daycare. Nenhum banco ou arquivo de configuração é apagado.

## O que mudou

Somente onze campos/constantes de apresentação:

- `Shirokai` → `Zorykai` nos títulos Reprodução, Chocadeira, Casal e Incubadora.
- Nome e descrição visuais do mod nos metadados.
- Nome no aviso de download do pacote visual, desconexão durante inicialização
  e identificação de erro de configuração que pode ser mostrado ao administrador.
- Prefixo padrão sem cor passa a usar nome branco em negrito e corpo cinza:
  `§f§l✦ Zorykai§r§7 » `. A capitalização existente é preservada.
- A ajuda `/breed help`, antes sem nome, usa o exemplo pedido:
  `§f§l✦ ZORYKAI§r§7 ...`.

Textos já coloridos e estilos dos menus mantêm suas formatações. O teste com o
formatador do Minecraft confirmou que os códigos de estilo não aparecem como
caracteres e que uma cor verde previamente definida no corpo continua verde.

## O que foi preservado

Código-fonte não foi editado ou recompilado para produzir o JAR. A alteração foi
feita nas constantes de texto binárias, com ajuste dos comprimentos UTF-8.
Todos os bytes após o constant pool de cada classe alterada são idênticos aos
originais: instruções, campos, métodos, anotações e atributos permaneceram iguais.
As demais constantes também são idênticas.

IDs, namespaces, comandos, permissões, placeholders, classes, pacotes, versão,
nomes técnicos de arquivos, mixins, persistência, dependências, cores dos menus,
URL e SHA-1 do pacote visual permanecem inalterados. As outras entradas do JAR
são idênticas byte a byte, inclusive a biblioteca SQLite embutida. As ocorrências
`Shirokai` em nomes técnicos e logs exclusivos de diagnóstico são intencionais.

## Prefixo já salvo no servidor

O mod preserva `config/shirokai-daycare/messages.json`. Se esse arquivo já
existir com `Shirokai` no campo `prefix`, o conteúdo salvo prevalece sobre o
novo padrão do JAR. Para atualizar apenas esse texto, com o servidor parado,
edite **somente o valor** de `prefix`, sem substituir o restante do JSON:

```json
"prefix": "§f§l✦ ZORYKAI§r§7 » "
```

Essa edição é opcional e não foi aplicada automaticamente a nenhum arquivo
existente. Se o prefixo já tiver suas próprias cores/degradês, troque apenas o
nome no valor atual para preservá-los.

O pacote de recursos é um ZIP externo. Seu desenho e a descrição na lista de
pacotes do cliente não foram modificados; os títulos de menu vêm do JAR e já
usam Zorykai. O endereço técnico do ZIP permanece o mesmo para manter compatibilidade.

## Verificação

- Comparação estrutural e byte a byte conforme [verification.json](verification.json).
- SHA-256 do resultado em [SHA256SUMS](SHA256SUMS).
- Inicialização real em Minecraft 1.21.1, Fabric 0.17.2, Java 21 e Cobblemon 1.8.3.
- Execução de `breed help` com o novo prefixo no servidor.
- Verificação do texto ordenado do Minecraft: marca branca/negrito, corpo padrão
  cinza e cores já selecionadas preservadas.

Esta é uma variante visual da mesma versão; não altera as regras do Daycare.
