# Ajuste do item na mão — 1.5.6

Relato: base grossa e globo suspenso acima da mão com Hold My Items.

Causa: GlobeItemRenderer forçava pedestal de 0.25, quatro vezes a espessura padrão (0.0625). O modelo de primeira pessoa adicionava translation Y=6, equivalente a 0.375 antes da escala do modelo.

Alterações: usar GlobeBaseModel.render com espessura padrão; retirar o deslocamento Y extra de primeira pessoa. A cena miniaturizada permanece com suas proporções originais e acompanha o pedestal.

Escopo: renderer do item e transformação de primeira pessoa. Renderização do bloco colocado e do exterior não foram alteradas.

Compilação: gradlew assemble, BUILD SUCCESSFUL. Não foram executados novos testes. O encaixe preciso na mão animada requer conferência visual do usuário.
