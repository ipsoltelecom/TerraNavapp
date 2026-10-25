# TerraNav 0.6-test — Música

- Controles de reprodução para as sessões de mídia do Spotify, YouTube e YouTube Music: faixa, artista, reproduzir/pausar, anterior/próxima e volume. Botões indisponíveis no player ficam desabilitados.
- Seleção automática do player em reprodução ou escolha explícita de um dos três aplicativos. A escolha explícita nunca controla outro player.
- Cartão Música nas Configurações e na tela Painel.
- Modo Música opcional para o joystick: direita avança e esquerda volta a faixa. No modo Mapa, os mesmos botões continuam controlando o zoom. Apenas esses dois códigos conhecidos foram associados à música; não foram presumidos códigos para pausar ou ajustar volume no guidão.
- Nome da faixa sobre o mapa enviado à moto quando o modo Música está ativo. Esta versão usa a interface TerraNav, sem integração com a tela nativa de música da Royal.

## Como usar

1. Atualize pelo TerraNav ou instale este APK por cima da versão anterior.
2. Abra Configurações → Música → Ativar acesso à música. Na tela do Android, habilite o acesso às notificações para **TerraNav — controles de música** e volte ao TerraNav.
3. Inicie a reprodução no Spotify, YouTube ou YouTube Music e escolha o player no TerraNav (ou deixe Automático).
4. Para trocar faixas pela moto, selecione **Música** em Modo do joystick. Essa opção volta para Mapa quando o acesso à mídia é desconectado ou o processo do app reinicia.

A reprodução em segundo plano depende das regras do próprio player e da conta. O TerraNav não desbloqueia reprodução em segundo plano do YouTube. O som continua saindo pela saída de áudio escolhida no celular, inclusive intercomunicador conectado.

## Privacidade e validação

O acesso às notificações permite obter as sessões de mídia pelo Android. O código não lê o conteúdo de notificações nem salva mensagens; metadados de música são mantidos em memória e exibidos localmente/no painel.

Compilação e testes automatizados realizados. A reprodução com contas reais dos três players e os comandos na moto ainda precisam de validação no aparelho do usuário. Nenhum comando de reprodução foi enviado durante a compilação.
