# Globe Dimension Community Port

Port comunitário do **Chunk In A Globe** para **Minecraft 1.21.1**, **NeoForge 21.1.259** e **Java 21**.

Entre em um globo, construa em seu espaço interno e saia para ver sua construção em miniatura dentro de um único bloco. Recolher e recolocar o mesmo globo mantém sua construção. De dentro, o mundo externo aparece ampliado ao redor da sala, somente como cenário visual.

Mantido por **Juan Marcos de Souza Texe — kikitotv**. Baseado no trabalho original de **modmuss50** e no fork comunitário de **cnlimiter / Nova Committee**. Veja [créditos](AUTHORS.md) e [licença MIT](LICENSE).

## Instalação

1. Instale Minecraft 1.21.1 com NeoForge 21.1.259 e Java 21.
2. Baixe o [JAR 1.5.4](releases/1.5.4/globedimension-1.5.4-neoforge.1.21.1.jar) e coloque-o na pasta `mods` do perfil. Em servidor dedicado, instale o mod no servidor e nos clientes.
3. Use somente uma versão do Globe no perfil e reinicie o jogo após substituir o JAR.

Os detalhes da versão e o checksum ficam em [releases/1.5.4](releases/1.5.4/LEIA-ANTES.md).

## Versão 1.5.4

A versão foi validada pelo mantenedor no jogo: miniatura colocada, construção interna, vista do mundo externo e permanência do piso/cenário ao girar a câmera.

- Interior independente: o exterior não é copiado para o espaço de construção.
- Reentrada preserva blocos e conteúdo de baús; o globo conserva seu ID ao ser recolhido.
- Iluminação da miniatura usa a posição de exibição; líquidos recebem a transformação correta.
- Paredes protegidas e retorno de segurança se o jogador ultrapassar os limites internos.
- Caixa de visibilidade do renderer cobre o cenário inteiro, evitando que piso e exterior desapareçam quando o bloco de saída fica fora do campo de visão.

As builds experimentais 1.5.2 que copiavam terreno externo foram substituídas. Globos usados nessas builds podem manter terreno já gravado: a atualização não limpa esse conteúdo automaticamente, pois ele pode estar misturado a construções legítimas.

## Desenvolvimento

Com um JDK 21 instalado:

```sh
./gradlew build
./gradlew test
./gradlew runClient
./gradlew runGameTestServer
```

No PowerShell, use `./gradlew.bat` no lugar de `./gradlew`. Os JARs compilados ficam em `build/libs`. O cliente de desenvolvimento usa `run`; os GameTests usam uma pasta separada, `run-gametest`.

Verificação registrada: **11 testes JUnit passaram** na 1.5.4, incluindo 3.600 combinações de câmera usando o `Frustum` real do Minecraft. Cinco GameTests passaram na revisão 1.5.3, incluindo sala nova vazia e preservação de blocos/baú na reentrada. A revisão 1.5.4 altera somente limites de visibilidade do renderer e testes relacionados.

## Documentação e limites

- [Correção da premissa e separação entre sala e cenário](docs/analysis/2026-10-22-premissa-e-correcao-1.5.3.md)
- [Diagnóstico e correção da visibilidade](docs/analysis/2026-10-22-frustum-1.5.4.md)
- [Histórico da conversão e decisões](docs/analysis/port-decisions.md)
- [Fila de regressões e verificações](docs/analysis/visual-regression-queue.md)

O cenário externo visto de dentro usa iluminação fixa; o payload ainda não transmite a iluminação remota. Modelos especiais de outros mods e block entities com renderização própria exigem validação específica. Os documentos de análise preservam o histórico, inclusive tentativas anteriores explicitamente superadas; caminhos de logs e fontes mencionados nesses documentos podem se referir ao workspace de desenvolvimento original.

Relate problemas nas [issues do repositório](https://github.com/devjuanmarcos/Globe-Dimension-Community-Port/issues), informando versão do mod, NeoForge, lista de mods e log/crash correspondente.
