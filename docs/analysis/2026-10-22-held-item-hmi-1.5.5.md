# Globe 1.5.5 — item invisível na mão com Hold My Items

Alvo: Minecraft 1.21.1 / NeoForge 21.1.259 / Java 21. Versão analisada da dependência opcional: arquivo `holdmyitemsnf-1.21.1v2.1.jar` usado no perfil de desenvolvimento (seus metadados internos informam 2.0).

## Causa confirmada

O globo funciona colocado no mundo e no inventário. Na primeira pessoa, Hold My Items chama `RenderHelper.shouldRenderCustom(ItemStack)`. Por ser BlockItem, o Globe entra no caminho que desenha diretamente os quads do modelo estático do bloco, através de `AlternateBlockRenderer.renderSingleBlockEmission`. O modelo estático do Globe não tem elementos: base e construção dependem de seu renderer de item registrado via `IClientItemExtensions`. Esse renderer era ignorado.

Isso explica por que o defeito ocorria tanto para um globo vazio quanto para um globo com construção. A espessura e a escala do pedestal não resolvem um renderer que nunca é chamado.

As fontes de `RenderHelper`, `HeldItemsMixin`, `BlockRenderManagerMixin` e a configuração da dependência foram inspecionadas com o MCP. O alvo público estático foi confirmado no bytecode: `shouldRenderCustom(Lnet/minecraft/world/item/ItemStack;)Z`. O MCP não garante remapeamento Mojang de JARs de mods; a confirmação usa o namespace real do JAR, e o Mixin usa `remap=false`.

## Correção

Mixin opcional e apenas de cliente sobre `RenderHelper`: quando o item é `GlobeBlockItem`, retorna false para a substituição por quads estáticos. Hold My Items então usa seu caminho normal de item, que chama o renderer do Globe com sua base e snapshot. Os demais itens continuam seguindo a regra original.

A integração não depende de Hold My Items para iniciar e não altera o arquivo de configuração do usuário. `@Pseudo` permite que o alvo esteja ausente; a configuração de mixin é opcional. O caso sem a dependência também é verificado em cliente separado.

Como alternativa manual para outras versões do Hold My Items, seu mecanismo de configuração oferece a lista `modRenderExclusions.renderBlockAsItem`. Adicionar `globedimension:globe`, preservando os valores existentes, também encaminha o item ao renderer adequado.

## Verificação de cliente

O comando opt-in `-PverifyHeldItem` ativa uma verificação na tela inicial, após os recursos carregarem, e encerra o cliente depois do teste. Não abre nem altera mundos.

```sh
./gradlew runClientWithHoldMyItems -PverifyHeldItem
./gradlew runClientWithoutHoldMyItems -PverifyHeldItem
```

O primeiro perfil usa `run-client-hmi` (com o JAR opcional na subpasta mods). O segundo usa `run-client-no-hmi`, sem esse mod. Os JARs de terceiros não são incluídos no repositório ou na distribuição do Globe.

- A verificação invoca o helper real do Hold My Items para globos vazio e com construção e exige que ambos usem o caminho de item. Um item de pedra comum deve manter seu comportamento original.
- O `ItemRenderer` real é chamado para primeira e terceira pessoa, nas duas mãos, e GUI. Um VertexConsumer conta a geometria: o pedestal vazio precisa emitir vértices, e o globo com snapshot precisa emitir geometria adicional.
- Reprodução vermelha: com a correção temporariamente desativada, o cliente terminou com `Hold My Items bypasses the globe item renderer`. A fonte corrigida foi restaurada em bloco finally.
- Cliente com Hold My Items: passou. Em cada um dos cinco contextos, globo vazio emitiu 24 vértices e globo com um bloco capturado emitiu 48. O teste também confirmou que pedra continua no caminho customizado original.
- Cliente sem Hold My Items: passou, com as mesmas contagens nos cinco contextos. A dependência opcional ausente não impediu a inicialização.
- Build final e onze testes JUnit passaram. Os cinco GameTests também passaram no servidor sem Hold My Items, conferindo que a configuração de mixin restrita ao cliente não introduz dependência de classes de cliente no servidor.

O teste confirma o encaminhamento e a geometria produzida pelo renderer real. A aparência final da mão animada no quadro do jogo ainda requer confirmação visual do usuário.
