# Continuação do diagnóstico do renderer interno — 22/10/2026

## Mudança nesta rodada

`GlobeBlockEntityRenderer` agora registra contexto adicional antes de relançar exceções de runtime ou erros durante a renderização de cada bloco/entidade capturados. Para blocos informa `globeId`, direção (`inner`), dimensão ativa, posição local e `BlockState`; para entidades informa tipo e UUID. Os `PoseStack` locais são desempilhados em `finally` antes de propagar a falha.

Isto é instrumentação de diagnóstico, não uma correção do crash. A evidência anterior continua sendo apenas o crash do item no inventário causado pelo handler do SecurityCraft; ainda não temos stack trace de falha ao entrar no interior.

## Build e artefato

- `compileJava jar` passou com Temurin 21.0.12.1.
- Não executei testes.
- Avisos existentes: `EventBusSubscriber.bus()` e `EventBusSubscriber.Bus` marcados para remoção em `GlobeClientRegistration.java`.
- JAR para reproduzir: `para-teste/globedimension-1.5.2-neoforge.1.21.1-render-diagnostics.jar`.
- SHA-256: `1CF2EA545F9687C9CBFD53273ECE79D4BEC276118A37ED86E022E78DE9490121`.
- Instruções de reprodução/coleta: `para-teste/LEIA-ANTES-RENDER-DIAGNOSTICO.md`.

## Próximo diagnóstico com o log

Se falhar em bloco, usar a mensagem `Globe snapshot block render failed` para separar problemas de color handler/model data/BlockEntity e reproduzir com o bloco identificado. Se falhar em entidade, usar `Globe snapshot entity render failed` para identificar o renderer/mod correspondente. A mudança mantém a exceção original; o stack trace continua no relatório normal.

O mapeamento de biome/luz segue como trabalho visual separado: o cliente não possui o `Level` da dimensão-fonte ao desenhar a outra dimensão. Uma correção fiel requer sincronizar contexto visual no payload em vez de consultar coordenadas locais do mundo ativo. A transformação de offsets também permanece sem alteração até comparação controlada com a posição do portal e evidência visual.
