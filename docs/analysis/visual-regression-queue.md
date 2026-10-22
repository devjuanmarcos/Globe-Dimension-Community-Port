# Fila de regressões visuais e de integração

Alvo: Minecraft `1.21.1`, NeoForge `21.1.259`.

| Prioridade | Sintoma | Estado | Diagnóstico/ação |
|---|---|---|---|
| Alta | Piso e cenário interno desaparecem ao olhar em certas direções | Caixa de visibilidade corrigida em 1.5.4; 3.600 combinações de câmera passaram; visual pendente | NeoForge testa `getRenderBoundingBox` mesmo em BERs globais. A caixa padrão de um bloco descartava toda a cena quando a saída saía do campo de visão. Agora a caixa cobre o cenário ampliado e a sala. Ver `2026-10-22-frustum-1.5.4.md`. |
| Alta | Vista do mundo exterior ao entrar no globo aparece deslocada ou inadequada | Premissa corrigida em 1.5.3; testes de geometria passaram; visual pendente | A escala 16 é necessária para o cenário externo, pois o espaço de construção representa um bloco externo. A saída fica no piso. A transformação correta omite o bloco central e posiciona os vizinhos além das paredes. Removida a cópia física do exterior. Ver `2026-10-22-premissa-e-correcao-1.5.3.md`. |
| Alta | Player sai do cubo interno, inclusive em modo criativo | Proteção e retorno de segurança implementados; build passa; validação visual pendente | Código original do port não bloqueava quebra das paredes Barrier/Globe nem colocação de Barrier. Logs mostram o player saindo dos limites do globo ID 5. A nova build cancela essas ações no servidor e reposiciona quem ultrapassar o cubo; a saída intencional continua sendo interagir com o Globe central. Também corrige teleportes absolutos enviados com flags relativas. |
| Alta | Build experimental importa terreno e sobrescreve o interior | Cópia removida em 1.5.3; sala vazia e preservação de baús/blocos verificadas no servidor | O interior contém construções do jogador, e o exterior é somente cenário visual. Globos contaminados pela build anterior não são limpos automaticamente para evitar apagar construções legítimas. |
| Alta | Miniatura colocada fica preta | Contexto de iluminação corrigido em 1.5.3; testes passaram; visual pendente | O renderer consultava a iluminação do mundo externo nas coordenadas locais 1..15, em profundidade. Agora usa o packedLight da posição de exibição e tint do biome nessa posição. |
| Alta | Líquidos aparecem fora da escala/posição do globo | Matriz aplicada em 1.5.3; testes passaram; visual pendente | LiquidBlockRenderer não aplica PoseStack. Um VertexConsumer transforma explicitamente os vértices, tanto na miniatura quanto no cenário externo ampliado. |
| Alta | Animais vistos do lado de fora ficam com animação estranha | Correção implementada; validação visual pendente | Separadas as atualizações de blocos e entidades. O cliente reutiliza entidades, interpola posição/rotação e atualiza a animação de caminhada. |
| Alta | Globo padrão, sem alterações, fica invisível quando segurado na mão | Ajuste de visibilidade aplicado; validação visual pendente | O item vazio desenhava uma base com 1/16 de bloco de espessura e depois recebia a escala do contexto de item. A base do item agora usa 1/4 de bloco; bloco colocado mantém sua espessura anterior. Conferir em primeira/terceira pessoa e com Hold My Items NF. |
| Alta | Globo padrão, sem alterações, fica invisível após ser colocado no chão | Correção implementada; validação visual pendente | A mesma correção do bloco-base se aplica ao renderer da BlockEntity. Testar recém-craftado e recém-colocado, antes de entrar nele. |
| Média | Compatibilidade com Hold My Items NF `holdmyitemsnf-1.21.1v2.1.jar`, usado em alguns perfis | Smoke de inicialização concluído; compatibilidade visual pendente | O cliente iniciou com o mod e carregou seus mixins. Ainda falta comparar o globo na mão com e sem Hold My Items NF instalado; não declarar compatibilidade visual até essa prova. |

## Roteiro de reprodução

1. Iniciar um mundo com o JAR do port e verificar o globo padrão na mão em primeira e terceira pessoa.
2. Colocar um globo recém-craftado, sem antes entrar nele, e verificar se a base aparece no chão.
3. Entrar no globo e inspecionar a área externa exibida em torno da saída.
4. Colocar vaca, ovelha ou galinha dentro da área capturada e observar de fora enquanto andam e olham em direções diferentes.
5. Repetir os passos 1 e 2 com `holdmyitemsnf-1.21.1v2.1.jar`; comparar com o perfil sem esse mod.

Ao receber `latest.log`, captura de tela ou gravação, associar a evidência a estes passos e atualizar os estados. Build e GameTests validam código e lógica, mas não substituem a confirmação visual no cliente.
| Alta | Inventário criativo fecha o jogo com base SecurityCraft Reinforced White Wool | Correção aplicada; validação no perfil pendente | O crash das 18:32 identifica `base_block=securitycraft:reinforced_white_wool`; o tint handler do SecurityCraft recebe `level=null`. `GlobeBaseModel` agora desenha o baked model diretamente, sem chamar `BlockColors`. Relatório: [crash-2026-10-21-18-32.md](crash-2026-10-21-18-32.md). |
| Alta | Bloco capturado com handler de cor que pressupõe mundo/BlockEntity | Fallback específico implementado; perfil real pendente | Os loops do item e do bloco colocado agora tentam o desenho normal e só usam cor neutra se a stack apontar para NPE em `BlockColors.getColor` chamado por `renderSingleBlock`; avisam uma vez por ID do bloco. Outros erros continuam aparecendo. Auditoria: [render-risk-audit-2026-10-21.md](render-risk-audit-2026-10-21.md). |
