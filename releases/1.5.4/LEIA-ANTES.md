# Chunk In A Globe 1.5.4

Minecraft 1.21.1 / NeoForge 21.1.259.

Corrigido o recorte que fazia o piso e o cenário desaparecerem quando o jogador olhava para longe do bloco de saída. O renderer passa a informar ao NeoForge a caixa da cena inteira, em vez de apenas um bloco.

Pode reutilizar o mesmo globo da 1.5.3. Substitua o JAR anterior e reinicie o Minecraft.

Verificação: onze testes JUnit passaram. A regressão de câmera exercita o Frustum real do Minecraft em 3.600 combinações de direção, inclinação, posição e campo de visão. A caixa antiga falhou nos testes; a caixa corrigida passou.

Teste visual: entre no globo e olhe para cima, para baixo e para todos os lados, caminhando até os quatro cantos. Piso e exterior devem continuar aparecendo mesmo quando o bloco de saída está atrás da câmera.

SHA-256: D76EAEA93199EC8B7A86D8908E5893DFE33D0F52F854990706237C112FA5D6F4
