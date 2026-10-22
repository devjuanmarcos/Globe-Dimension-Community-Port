# Revisão da renderização do Chunk In A Globe

Alvo do port: Minecraft 1.21.1, NeoForge 21.1.259, Java 21. MCP `minecraft-modding` não está disponível neste host; análise baseada nas fontes locais do original/Fork/port e documentação/código 1.21.1 consultados na web.

## Diagnóstico do deslocamento visto de dentro

Ao entrar, o bloco de saída é criado em `globeOrigin + (8, 0, 8)`. O snapshot do exterior usa `origin = returnPos + (-8,-8,-8)` e codifica suas células com coordenadas locais 1..15. Portanto, a célula central `(8,8,8)` corresponde à posição do bloco de saída e deve ser alinhada ao BER desse bloco com a translação `(-8,-8,-8)` aplicada antes da escala 16.

O renderer aplicava também `(-7.5,0,-7.5)` em coordenadas de mundo depois de escalar a translação central. Isso movia toda a paisagem 7,5 blocos para X/Z em relação à saída. O deslocamento adicional foi removido em `GlobeBlockEntityRenderer`; permanece necessária a validação visual em jogo.

## Limite estrutural do renderer atual

O port recebe somente `BlockState` por célula e chama `BlockRenderDispatcher.renderSingleBlock`. No código consultado para o dispatcher, esse método obtém um modelo e submete suas faces no caminho de renderização de item, sem `BlockAndTintGetter` nem `BlockPos`. O caminho em lote (`renderBatched`/tesselação do modelo) recebe explicitamente esses contextos. A API `BlockAndTintGetter` é a interface de mundo que fornece estados vizinhos e cor/tinta para os modelos.

Consequências esperadas do método atual: faces internas entre blocos opacos ficam desenhadas; água/fluidos não usam o renderizador de fluido do mundo; tintas por biome e modelos que dependem de vizinhos/BlockEntity/model data perdem contexto. Isso explica por que apenas corrigir a posição não torna a paisagem igual a chunks normais.

## Fluxos avaliados

1. **Snapshot com tesselação contextual local (recomendado para o próximo passo).** Manter a rede limitada de 15³ estados, construir no cliente uma visão local imutável que responda aos estados vizinhos, biome/tinta e luz, e tessellar por render layer com culling de faces. Adicionar captura/serialização dos dados necessários para block entities e fluidos por etapas. Vantagem: preserva o desenho compacto e o protocolo de snapshot. Limitação: custom renderers de blocos e dados específicos de mods podem precisar de adaptações.
2. **Renderizar a dimensão/chunks reais via câmera e framebuffer.** A cena seria renderizada como mundo vanilla e apresentada numa superfície. Dá maior fidelidade a luz, fluidos, BEs e entidades, mas muda o conceito do globo, exige câmera/cena isolada, recursão/culling, atualização de chunks e integração delicada com pipelines como Fabulous/Iris/Oculus. Não é uma substituição pequena.
3. **Continuar com `renderSingleBlock`.** Baixo custo, mas não resolve as limitações estruturais descritas acima; adequado somente para miniaturas aproximadas.

Compact Machines é uma referência útil de salas dimensionais em NeoForge 1.21.1, mas o repositório aberto não demonstra que seu fluxo de render é transferível diretamente para esse efeito de miniatura. A decisão acima se apoia sobretudo na separação explícita no dispatcher entre renderização de um modelo isolado e tesselação de bloco com contexto de mundo.

## Item vazio na mão

O modelo do item usa `builtin/entity`, tem renderer BEWLR registrado em `RegisterClientExtensionsEvent` e o relatório de crash anterior comprova que `GlobeItemRenderer.renderByItem` realmente é invocado no inventário. O item sem `globe_id` ainda renderiza a base padrão de tábuas de carvalho; ela tinha somente 1/16 bloco de altura antes da escala de mão (0,4 em primeira pessoa). Isso torna o perfil final extremamente fino. A altura da base no item foi aumentada para 1/4 de bloco; a base do bloco colocado não foi alterada.

O registro BEWLR está de acordo com a documentação NeoForge 1.21.1. Um mod de pose/renderização como Hold My Items pode mudar a matriz ou o contexto visual; não há evidência nos arquivos atuais de que ele impeça a chamada ao renderer. A confirmação precisa ser feita nos dois perfis.

## Referências

- NeoForge 1.21.1, [Block Entity Renderer e BlockEntityWithoutLevelRenderer](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/).
- Código de `BlockRenderDispatcher` consultado: [renderSingleBlock e renderBatched](https://github.com/H1lkaaaGD/Minecraft_1.21.11_Source/blob/main/net/minecraft/client/renderer/block/BlockRenderDispatcher.java). A página é uma árvore de código 1.21.11; nomes e separação dos métodos foram cruzados com os mapeamentos 1.21.1, então o corpo de 1.21.11 é apenas referência conceitual, não prova de equivalência binária.
- [BlockAndTintGetter nos mapeamentos 1.21.1](https://mappings.xhyrom.dev/1.21.1/net/minecraft/world/level/blockandtintgetter).
- [Compact Machines (fonte NeoForge; versão 1.21.1 listada pelo projeto)](https://github.com/CompactMods/CompactMachines).

## Próxima implementação recomendada

Separar primeiro a representação da seção de seu desenho. Fazer um `BlockAndTintGetter` local, testar a tesselação de cubos contíguos, faces, estados tintados e fluidos, e então ativar esse caminho na renderização em miniature. A rede precisará carregar biome/luz e dados de BlockEntity se os casos correspondentes forem requisito. Isso é um trabalho de renderer, não apenas uma correção de offsets. Ainda não se declara que a visualização completa está corrigida.
