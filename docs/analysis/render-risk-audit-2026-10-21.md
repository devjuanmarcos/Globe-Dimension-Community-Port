# Auditoria de riscos do fluxo de renderização — 21/10/2026

Alvo: Minecraft 1.21.1, NeoForge 21.1.259. Evidências: fontes do port e `latest.log (1).txt` / `crash-2026-10-21_18.32.27-client.txt` enviados pelo usuário.

## Caminhos cobertos

| Caminho | Entrada | Renderer |
|---|---|---|
| Item vazio ou com base escolhida | `CUSTOM_DATA.base_block` ou oak planks padrão | `GlobeItemRenderer` -> `GlobeBaseModel` |
| Conteúdo capturado mostrado no item | até 15³ `BlockState` recebidos pelo payload | `GlobeItemRenderer` -> `GlobeBlockModelRenderer` |
| Globo colocado, base e moldura interna | `GlobeBlockEntity` e base persistida | `GlobeBlockEntityRenderer` -> `GlobeBaseModel` |
| Conteúdo capturado mostrado no bloco | até 15³ `BlockState` recebidos pelo payload | `GlobeBlockEntityRenderer` -> `GlobeBlockModelRenderer` |

## Falha confirmada e alcance

O crash das 18:32 foi ao renderizar `globedimension:globe` com `base_block=securitycraft:reinforced_white_wool`. A cadeia de erro passa por `BlockColors.getColor` sem `BlockAndTintGetter` e posição. O código antigo da base e os dois loops da miniatura usavam `BlockRenderDispatcher.renderSingleBlock`, que pede cor no mesmo contexto nulo. Portanto, um bloco capturado cujo color handler suponha mundo/BlockEntity poderia causar o mesmo tipo de crash.

O log lista providers de cor do SecurityCraft para `reinforced_dispenser`, `reinforced_dropper`, `reinforced_hopper`, `reinforced_observer`, `reinforced_grass_block` e `reinforced_water_cauldron`; o crash report prova `reinforced_white_wool` como base do item. Isso identifica uma família de risco no perfil, mas não prova que todos esses handlers falhariam na mesma chamada.

## Proteção implementada

- A base usa o baked model diretamente, com cor neutra, evitando handlers contextuais sem mundo. A textura continua; tintas dinâmicas ou específicas de mods não são aplicadas à base.
- As duas miniaturas de conteúdo tentam primeiro o render normal para preservar cores quando o handler aceita contexto nulo.
- Se ocorrer `NullPointerException` e a stack incluir simultaneamente `BlockColors.getColor` e `BlockRenderDispatcher.renderSingleBlock`, o helper desenha o baked model com cor neutra e registra um aviso por ID do bloco. Outros NPEs e outras exceções são relançados.
- Os renderers de item e bloco colocado compartilham o helper.

## Outros pontos que podem falhar ou aparecer incorretos

1. **Dados de BlockEntity.** O snapshot serializa estados de bloco e entidades vivas, mas não carrega BlockEntity NBT/model data. Displays, máquinas, inventários e modelos dinâmicos podem ficar incompletos. Não há evidência de que isso tenha causado os crashes recebidos.
2. **Fluidos.** Os payloads guardam `BlockState`, não o `FluidState` completo, e os renderers desenham modelos de bloco. Água/lava podem sumir ou não respeitar vizinhança/transparência.
3. **Tint de bioma/contexto.** O fallback usa cor neutra. Blocos dependentes de bioma/estado do mundo podem aparecer com cor diferente; a base também usa neutro.
4. **Modelos especiais.** Um modelo que dependa de model data, BlockEntity ou Level pode falhar fora do fallback específico de `BlockColors`. Esses erros continuam visíveis para que a próxima stack trace identifique a causa.
5. **Tag de blocos-base.** A aba criativa enumera `globedimension:base_blocks`, que inclui tags vanilla e pode receber extensões de datapacks/mods. Cada bloco adicionado pode ter renderização especial; não há como garantir suporte visual universal só pela lista estática.
6. **Mixins do perfil.** O log mostra vários mods transformando `BlockRenderDispatcher`, inclusive Hold My Items NF, Botania, LDLib, Aeronautics e EnderIO. A stack atual aponta o acesso nulo do color handler do SecurityCraft; presença de mixin não prova que ele causou a falha.
7. **Renderers de entidades capturadas.** O snapshot pode incluir até 32 entidades não jogadoras e o cliente chama o renderer registrado para cada tipo. Um modded entity renderer com premissas de mundo/BlockEntity pode ser outra fonte de crash; não aparece nesta stack de item e precisa de evidência própria.

## Verificação desta rodada

`compileJava` e `jar` passaram. Nenhum teste foi acrescentado ou executado nesta rodada. O JAR atualizado está em `../para-teste/globedimension-1.5.2-neoforge.1.21.1.jar`. A proteção precisa ser confirmada no perfil CurseForge com a lã reforçada branca como base e blocos SecurityCraft dentro da região capturada. O helper escreverá no `latest.log` quando desenhar algum conteúdo sem a tinta contextual.

## Próxima etapa técnica

Para fidelidade visual além de evitar o crash, substituir o `renderSingleBlock` por tesselação de uma seção local com estados vizinhos, dados de biome/luz e tratamento explícito de fluidos. BlockEntity/model data requer protocolo próprio e limites. A proteção atual não afirma paridade completa com renderização vanilla.
