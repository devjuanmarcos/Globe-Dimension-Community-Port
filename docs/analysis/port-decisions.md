# Auditoria e decisões do port

## Perfil confirmado

- Alvo: Minecraft `1.21.1`, NeoForge `21.1.259`, Java `21`.
- Mod ID/namespace: `globedimension`.
- Fork 1.20.1: branch `multi/1.20.1`, commit `d5be66e0f96c1a783dd29e79fd1006fbebed317c`.
- MCP Minecraft: indisponível nesta sessão; nenhum tool MCP de modding aparece no host. Fallback: fontes preservadas e documentação/MDK oficial de 1.21.1.

## Base e evidência por função

O fork 1.20.1 é a base estrutural candidata porque já separa lógica comum e integrações de loader e contém código mais recente que a fonte original 1.16.5. O módulo Forge só serve como referência conceitual: usa ForgeGradle, pacotes `net.minecraftforge.*` e `mods.toml`. A implementação escolhida é NeoForge loader único, adaptando `common/` e consultando o original/release JARs para resolver diferenças. A comparação estrutural e de bytecode das classes centrais com o release `1.20.1-1.5.1` está registrada em `release-1.20.1-1.5.1-comparison.md`; a revisão semântica classe a classe ainda está aberta.

| Área | Evidência do fork | Direção para 1.21.1 |
|---|---|---|
| Inicialização e IDs | `common/CommonClass.java`, `Constants.java`, `ModRegistries.java` por loader | Reescrever entrypoint e registros como um único mod NeoForge; preservar `globedimension:globe`, receita e codec de gerador. |
| Globo e interação | `common/block/GlobeBlock.java`, `common/tile/GlobeBlockEntity.java`, `common/item/GlobeBlockItem.java` | Manter ID/base block no dado persistente; servidor controla criação do ID e teleportes. Validar entidade/jogador antes de cast e tratar campos ausentes. |
| Área interna | `utils/GlobeSection.java`, `GlobeManager.java`, gerenciadores client/server | Preservar moldura/região de entidade 17×17×17; blocos são copiados apenas em coordenadas relativas 1..15 por eixo (15³ células), entidades consultadas numa AABB 17³. Validar dados e limites ao restaurar. |
| Dimensão | `common/world/VoidChunkGenerator.java`, `common/dim/*`, JSON em `data/globedimension/dimension*` | Atualizar codec, registro e JSON com o schema vanilla 1.21.1 e as assinaturas compiladas contra NeoForge 21.1.259. |
| Receita e tag | `common/crafting/GlobeCraftingRecipe.java`, `data/globedimension/recipes/globe.json`, tag `base_blocks` | Migrar serializer/receita e tag ao schema 1.21.1; aceitar bloco-base apenas se elegível. |
| Rede | `common/net/SectionUpdatePkt.java`, `UpdateRequestPkt.java`; adaptadores por loader | Substituir pelo payload API NeoForge. O pedido antigo lê `amount` e itera sem limite; não manter esse parser sem limite. Aplicar direção, tamanho, mundo e autorização. |
| Cliente | `common/client/render/*`, mixins do common e configuração Forge/Fabric | Isolar renderer no setup de cliente. Investigar necessidade do mixin Entity e mixin de item; não adicionar Mixin até confirmar alvo exato e ausência de hook API. |
| Licença | `LICENSE` em ambos snapshots | Copiar MIT e conservar avisos/créditos no projeto derivado. |

## Fontes de API verificadas

- Documentação oficial NeoForge 1.21.1 Getting Started declara JDK 21 e direciona a usar MDK 1.21.1; o MDK ModDevGradle oficial atual usa `net.neoforged.moddev` e toolchain 21. A versão do NeoForge no template deve ser fixada em `21.1.259`.
- Documentação oficial de Networking 1.21.1 registra payloads via `RegisterPayloadHandlersEvent`; consultar as páginas de payload/stream-codec e o código compilado da versão exata ao implementar.
- Download Maven oficial foi consultado e contém o artefato `net.neoforged:neoforge:21.1.259`.
- Fontes: https://docs.neoforged.net/docs/1.21.1/gettingstarted/ ; https://docs.neoforged.net/docs/1.21.1/networking/ ; https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle ; https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml

## Estado da implementação NeoForge (21/10/2026)

Foi criado um port inicial em `../ports/neoforge-1.21.1-21.1.259/`. Registros, receita, dados da dimensão void, SavedData de IDs, item/bloco com BlockEntity, teleporte, sincronização de blocos e entidades não jogadoras, renderer de bloco/item e pedido autenticado de atualização do item estão implementados em versão inicial. O carregamento dos quatro chunks da região tem retenção temporária de 10 segundos. Há teste unitário para os limites 15³/17³.

Verificação executada: `gradlew.bat clean build --no-configuration-cache` passou com JDK Temurin 21.0.12.1+1; o artefato foi criado. O cliente chegou ao carregamento de recursos, criou os atlases e carregou 1291 receitas/1399 advancements; não foi feita inspeção visual de um mundo. O servidor dedicado carregou o mesmo mod e chegou a `Done`, criando os dados da dimensão `globedimension:globe`; não houve jogador conectado nem teste de crafting/teleporte, sincronização multiplayer ou save/reload.

Limitações conhecidas: renderer visual ainda não foi inspecionado dentro do jogo; a comparação encontrou e corrigiu o retorno precoce do renderer quando o globo não tinha ID, que também impedia a base de ser desenhada. Entidades jogadoras são deliberadamente excluídas para não serializar perfil/inventário do jogador como fazia o código de origem; entidades visuais são snapshot e limitadas a 32 com tamanho individual limitado; falta validar ciclos de item/teleporte, sincronização multiplayer, persistência após reabrir o mundo e uso concorrente de retornos. A BlockEntity envia update packet/tag para o cliente quando os metadados mudam.

## Decisões confirmadas e pendências

- Confirmado: iniciar como NeoForge de loader único, namespace `globedimension`, Java 21; não transportar Forge-only `ibe-editor` sem prova de uso funcional.
- Confirmado: payloads de sincronização devem ser limitados e validados; o cliente não escreve estado autoritativo.
- Parcialmente concluído: comparar release JAR 1.20.1-1.5.1 com o fork por inventário, recursos e bytecode central; faltam métodos de receita/renderização/eventos e auditoria completa de managers/event handlers.
- Pendente: confirmar visualmente os renderers em jogo; a proteção de explosões usa `ExplosionEvent.Detonate` NeoForge, substituindo o mixin de explosão, e o item usa `RegisterClientExtensionsEvent`.
- Concluído nesta rodada: GameTests executados no NeoForge `21.1.259`; 3 testes obrigatórios passaram. A classe `GlobeRuntimeGameTests` verifica codec, BlockEntity/metadados/update packet e limites de alocação. O GameTestServer usa nível isolado e não serve para comprovar registro/uso da dimensão; esse caso foi verificado em servidor dedicado recém-gerado.
- Pendente: auditoria semântica completa, teste jogável de crafting/colocação/quebra/entrada/saída, renderer em mundo real, save/reload e dois clientes. Os testes atuais não demonstram paridade funcional completa.
