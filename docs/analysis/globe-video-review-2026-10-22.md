# Revisão da gravação do interior do Globe — 22/10/2026

> CORREÇÃO DE PREMISSA em 1.5.3: a escala 16 do cenário externo é intencional, pois cada bloco externo equivale a 16 blocos internos. As conclusões abaixo que recomendam escala 1 para esse cenário estavam erradas. O problema era seu alinhamento ao bloco central e a posterior cópia física indevida do exterior. Ver [correção atual](2026-10-22-premissa-e-correcao-1.5.3.md).

Vídeo: `C:\Users\Marcos\Videos\Captures\Minecraft NeoForge_ 1.21.1 - Singleplayer 2026-10-22 16-24-30.mp4`
Execução: Minecraft 1.21.1, NeoForge 21.1.259, Globe 1.5.2 e Hold My Items NF 2.1.

## Falhas visuais observadas

Captura posterior da regressão da placa: [2026-10-22-interior-base-anchor-regression.png](captures/2026-10-22-interior-base-anchor-regression.png).

1. **A paisagem cresce muito e atravessa a câmera.** O payload salva `BlockPos` locais inteiros de 1 a 15. O renderer interno multiplicava todas as coordenadas e cada modelo por 16, transformando uma região de 15 blocos em até 240 blocos, enquanto a sala/barreira tem 17 blocos. Nos quadros em torno de 8 s o jogador vê só o céu; em torno de 14 s a grama ocupa a câmera como planos enormes.
2. **A cena está ancorada longe do portal.** `GlobeRenderTransform` ainda aplicava `(-7.5,0,-7.5)` antes da escala 16. Na matriz final, esse deslocamento vira 120 blocos em X e Z. Os dados 1..15 com translação `-8` deveriam ficar em volta do bloco de saída, não a centenas de blocos dele.
3. **A base ampliada fica fora do centro.** A captura posterior de 16:35 confirmou que `(-0.5,0,-0.5)` deixa o bloco unitário escalado para 16×16 deslocado cerca de 7 blocos em relação à placa de saída. Como o bloco-base ocupa `[0,1]` antes da escala, a translação correta é `(-7.5,0,-7.5)`: o piso passa a ocupar `[-7.5,8.5]`, centrado no centro do bloco de saída em `0.5`.
4. **Os contornos vermelhos de Barrier no final não são um defeito do Globe.** O jogador seleciona o item Barrier na barra rápida; o Minecraft mostra esses contornos quando esse item está na mão em modo criativo.

## Cruzamento com o log

O log registra snapshot estável de 1.580 blocos e a câmera se movendo dentro da região. Não encontrei `Globe snapshot block render failed`, `Globe snapshot entity render failed` nem exceção de renderer nesta execução. Portanto, a gravação evidencia um erro determinístico de escala/transformação, não o crash de item causado pelo SecurityCraft visto no relatório anterior.

## Correção aplicada

- A cena interna agora usa escala `1.0` e offsets `(-8,-8,-8)`: cada célula capturada ocupa um bloco e o volume 1..15 cabe no espaço interno de 17³.
- A primeira correção usou `(-0.5,0,-0.5)` incorretamente. A captura seguinte confirmou o deslocamento; o código foi corrigido para `(-7.5,0,-7.5)`, mantendo o piso centralizado no bloco de saída.
- A captura nova ainda mostra defeitos na relação visual entre terreno e piso; essa alteração corrige a âncora identificada, mas não valida a renderização completa do exterior. Escala, recorte, iluminação e demais falhas precisam de nova conferência dentro do jogo.
- `compileJava jar` passou com Temurin 21.0.12.1; não rodei testes.
- JAR de teste: `para-teste/globedimension-1.5.2-neoforge.1.21.1-interior-scale-fix.jar` (SHA-256 `18E779EDAFF8C11E57E40DC3DFB1ECB036BAF439ED2F44D5B8DAF73D1C9579F7`).
- A mesma cena ainda precisa ser repetida visualmente. Isso não resolve, por si só, a falta de dados de BlockEntity/model data nem a correspondência de biome/luz/fluidos.

## Validação recomendada

Reabrir o mundo de desenvolvimento e repetir o percurso do vídeo: sair do portal, olhar ao redor e olhar para baixo. Confirmar que o terreno não aumenta ao aproximar-se, permanece em volta do jogador e que a base forma o piso centrado. Repetir com Barrier fora da mão para avaliar a geometria sem os contornos de depuração.
