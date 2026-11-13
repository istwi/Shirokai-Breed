# Shirokai Daycare — candidato de homologação

Arquivo: `shirokai-daycare-0.1.0-dev.jar`.

Servidor: Minecraft 1.21.1, Java 21, Fabric Loader 0.17.2, Fabric API
0.116.6+1.21.1, Cobblemon 1.8.3, Impactor Fabric 5.3.5 e LuckPerms Fabric 5.4.140.
Instale o JAR somente no servidor, na pasta `mods/`.

Este candidato implementa `/breed` e `/chocadeira`, sem ovos na party.
Não é um release aprovado para produção. Passaram 27 testes automatizados e
71 verificações de API/serviço no servidor dedicado. Faltam homologação com
cliente autenticado, testes reais de rede/crash e carga multiplayer.
Compras interrompidas sem confirmação exigem conciliação administrativa.

Confira SHA-256 em `SHA256SUMS`. SQLite JDBC está aninhado com seus avisos de
licença; as demais dependências devem ser instaladas separadamente.
