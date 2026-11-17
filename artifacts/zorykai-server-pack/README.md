# Zorykai 0.1.12-dev — pacote combinado pelo servidor

JAR: `shirokai-daycare-0.1.12-dev-zorykai-server-pack.jar`.

Base: variante Zorykai 0.1.12-dev da revisão
`28a2bd6c592d7f30ebeb781e844d544bc530f224`, a mais recente encontrada nas
branches remotas `main` e `zorykai` ao iniciar esta correção. Não havia tags
remotas. Esta é uma correção local dessa versão, não um novo release oficial.

## Instalar com seu ZIP que reúne duas texturas

1. Pare o servidor e guarde uma cópia do JAR atual e das configurações.
2. Substitua o JAR Zorykai antigo pelo JAR deste diretório. Mantenha apenas
   **um** JAR do Daycare em `mods/`.
3. Em `config/shirokai-daycare/config.json`, altere somente:

   ```json
   "resourcePackEnabled": false
   ```

4. Em `server.properties`, mantenha a URL do seu **ZIP combinado** em
   `resource-pack` e configure este ID exato:

   ```properties
   resource-pack-id=cd77918b-6448-4bb4-8c7f-de4fda6a318a
   ```

   Mantenha `resource-pack-sha1` com o SHA-1 dos bytes desse mesmo ZIP combinado.
   Não use o SHA-1 do pacote Shirokai separado. Para calcular:

   ```sh
   sha1sum /caminho/do/pacote-combinado.zip
   ```

   Copie somente os 40 caracteres do hash para o valor de
   `resource-pack-sha1`. Atualize-o quando mudar o conteúdo do ZIP servido.
   Preserve as demais propriedades e as duas texturas dentro do pacote.

5. Reinicie, reconecte, aceite o pacote se solicitado e abra os menus do Daycare.
   Reconecte uma segunda vez para conferir o reaproveitamento do cache e a
   ausência de uma segunda oferta pelo mod.

O Daycare deixa de oferecer um segundo pacote quando sua opção está desativada.
Ele só ativa o visual após receber `SUCCESSFULLY_LOADED` para o ID acima.
Falha, recusa ou ausência de confirmação não são tratadas como sucesso.
O ID é uma identificação configurada pelo administrador, não uma inspeção do
conteúdo do ZIP: o pacote combinado precisa conter os recursos do Daycare.
Downloads repetidos por URL instável, hash incorreto ou outro mod que também
envia pacotes continuam exigindo diagnóstico no servidor.

## Alteração e validação

- A confirmação é capturada também durante a fase de configuração, antes de
  o jogador entrar no mundo.
- Com o envio do mod desativado, a entrada não apaga a confirmação externa.
- Cada nova sessão de configuração limpa o estado anterior; o desligamento
  mantém a limpeza existente.
- O envio original pelo mod continua disponível com `resourcePackEnabled: true`.
- Branding Zorykai, regras de reprodução, banco, SQLite embutido, metadados,
  comandos e recursos permanecem byte a byte iguais nas entradas não alteradas.

Passaram 23 verificações JVM com stubs explícitos do protocolo Minecraft.
Também passou a aplicação real dos mixins no Minecraft 1.21.1 / Fabric 0.17.2,
com verificação JVM, execução dos callbacks injetados, pacote de status real e
limpeza na reconexão. Esse teste isolado sai antes de iniciar o servidor: não
é um teste completo do mod com Cobblemon, login de jogador, cache do cliente,
renderização ou do seu ZIP combinado. Esses pontos precisam de homologação.
Detalhes e hashes em `verification.json` e `SHA256SUMS`.

O repositório distribui binários e não inclui o projeto-fonte do mod. A alteração
foi feita de forma restrita no bytecode, com ferramenta reproduzível em
`../../tools/PatchServerPack.java`. Não instale o mod de teste
`zorykai-linkage-probe.jar` no seu servidor.
