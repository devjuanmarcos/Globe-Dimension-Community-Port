# Comparação da release Fabric 1.20.1-1.5.1 com o fork e o port

Data: 2026-10-21
Alvo do port: Minecraft `1.21.1`, NeoForge `21.1.259`.

## Escopo e fontes

- Binário: `releases/Chunk-In-A-Globe-fabric-1.20.1-1.5.1.jar` (SHA-256 no `manifest.yml`).
- Código do fork: `source-multiloader-1.20.1/common/src/main/java` e recursos `common/src/main/resources`.
- Port: `ports/neoforge-1.21.1-21.1.259/src/main`.
- Inventários reproduzíveis: `release-1.20.1-1.5.1-jar-contents.txt` e `fork-common-java-inventory.txt`.
- Saída parcial de bytecode: `release-1.20.1-1.5.1-core-bytecode.txt` (`javap -p -c` para BlockEntity, manager, seção, payloads e placers).

Esta é uma auditoria estrutural com inspeção de bytecode das classes centrais, não uma prova de equivalência completa. O JAR usa nomes intermediários do Fabric/Yarn, enquanto o port usa Mojang mappings; comparação por hash de classes não seria significativa.

## Achados

1. **A release contém a implementação comum do fork mais a integração Fabric.** O JAR contém as classes comuns de bloco, BlockEntity, receita, dimensão, seção, teleportes, rede e renderização; também inclui helpers e packet classes específicos de Fabric, `fabric.mod.json` e dois arquivos de configuração Mixin. O port remove essas integrações e registra os equivalentes NeoForge.
2. **Os recursos centrais mantêm os mesmos identificadores.** O JAR e o fork usam `globedimension:globe`, `globedimension:globe_type`, a receita `globedimension:globe` e a tag `base_blocks`. O port preserva IDs e adapta a localização singular da tag/receita ao layout 1.21.1. JSON de dimensão comparado entre fork e port é igual.
3. **A semântica espacial observada coincide.** `GlobeSection` do fork varre coordenadas internas 1..15 (15³ blocos) e consulta entidades numa AABB 17³. O port mantém essas dimensões; limita entidades transmitidas a 32 e exclui jogadores, além de remover `Passengers` e impor limite de tamanho NBT.
4. **A release serializa IDs e dados de retorno na BlockEntity; o port conserva esses campos.** O bytecode de `GlobeBlockEntity` mostra `globe_id`, bloco-base e coordenadas/dimensão de retorno. O port mantém o formato lógico em seus próprios nomes de chave. Não há garantia de compatibilidade de save entre versões.
5. **Persistência de conteúdo é limitada também no fork.** `GlobeManager.Globe.fromTag` e `toTag` no código do fork são vazios; a seção é reconstruída a partir do mundo observado em runtime. O port também captura conteúdo dinamicamente e não promete migração de conteúdo ou save entre versões.
6. **O esquema de distribuição de chunks mudou.** Fork/release alocam um chunk por globo, com espaçamento de 100 chunks; o port reserva uma região 17³ e usa espaçamento de 1600 blocos, além de forçar até quatro chunks e soltá-los por timeout. IDs já gravados em saves do fork não podem ser presumidos compatíveis com a nova geometria.
7. **O protocolo de rede foi redesenhado.** A release usa os packets Fabric `SectionUpdatePkt` e `UpdateRequestPkt`; o port define payloads NeoForge, limita o pedido a um ID, valida posse do item e ID no servidor e limita o conteúdo transmitido. Não há compatibilidade de protocolo entre loaders.
8. **O port preserva assets visuais encontrados no JAR.** Blockstate, modelos, textura do portal e traduções en/zh estão em `src/main/resources`. O renderer é implementação própria para NeoForge e ainda precisa de inspeção visual no jogo.
9. **Falha de renderização encontrada e corrigida na revisão do renderer.** O renderer NeoForge retornava imediatamente quando `globe_id` era `-1`, estado normal de um globo recém-craftado antes do primeiro uso. Como o bloco usa `ENTITYBLOCK_ANIMATED`, isso também pulava o desenho da base e podia deixar o bloco invisível. O retorno foi movido para limitar apenas a captura de conteúdo; a base é desenhada sempre. O renderer original desenhava a base independentemente do ID. A correção compila e passa build/JUnit e os 3 GameTests; a aparência ainda precisa de conferência visual no cliente.

## Cobertura semântica desta revisão

| Fluxo | Referência | Port atual | Estado / diferença relevante |
|---|---|---|---|
| Crafting especial | Receita usa 5 vidros, centro vazio, 3 itens do mesmo bloco na tag-base | Mesmo padrão e tag; saída contém base no `CUSTOM_DATA` | Estruturalmente equivalente; conferir crafting em jogo continua pendente. |
| Colocar e quebrar | Item aplica `base_block` e `globe_id`; bloco dropa dados do BE e clone recupera o item | Usa Data Components `CUSTOM_DATA`; drop manual e loot vazio | Mantém campos lógicos; validar que não há duplicação/perda no ciclo do item em jogo. |
| Atribuir ID e entrar | Primeiro uso aloca ID e constrói moldura 17³ com portal | Primeiro uso aloca ID no `SavedData`, força chunks e constrói estrutura | Fluxo preservado com allocator sequencial e geometria NeoForge própria. |
| Sair | BE interno usa dimensão/posição de retorno gravadas | Serviço procura posição livre acima do retorno, com fallback no spawn | Mais defensivo; colisão/segurança do ponto ainda exige jogador real. |
| Snapshot de blocos | Interior 15³, atualizado em intervalos | Mesmo interior 15³ em cada atualização de seção | Conteúdo de bloco preservado; frequência/custo e carga precisam de medição prática. |
| Snapshot de entidades | Release serializa entidades e jogadores | Limite de 32, exclui jogadores, remove passageiros e limita NBT individual | Diferença intencional de privacidade/robustez; entidades dinâmicas são só snapshots. |
| Renderer do bloco | Desenha conteúdo se houver ID e sempre desenha a base | Corrigido para desenhar a base mesmo sem ID | Código compila; renderização real não foi inspecionada. Transformações internas seguem pendentes de comparação visual. |
| Renderer do item | Usa base e solicita conteúdo quando o item tem ID | Mesmo propósito via renderer NeoForge e payload autenticado | API reescrita; inspeção de item em GUI/mão e resposta multiplayer pendentes. |
| Mixin de explosão e evento de cliente | Release usa mixin de Entity e renderer item via mixin de modelo vanilla | Proteção usa `ExplosionEvent.Detonate`; item usa extensão de cliente NeoForge | Estratégia substituída por hooks do loader; confirmar comportamento no jogo e ausência de regressão. |

## Decisão

A release 1.20.1-1.5.1 confirma IDs, assets e comportamento estrutural usados como referência. O fork permanece útil para entender lógica comum, mas não é base binária compatível. O port atual é uma reimplementação NeoForge; não é conversor de saves nem garante paridade completa de renderização, entidades, teleportes ou conteúdo.

## Pendências de auditoria

- Comparação semântica de classes centrais ampliada nesta revisão; falta auditoria bytecode/método integral das classes restantes do JAR e do fork.
- Verificar comportamento dentro do jogo: crafting, colocação/quebra, entrada/saída, conteúdo renderizado, save/reload e dois clientes.
- Conferir licenças/créditos de todos os arquivos efetivamente derivados antes de qualquer redistribuição.
