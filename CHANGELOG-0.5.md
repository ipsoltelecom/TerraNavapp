# TerraNav 0.5-test

- Garagem: os cinco serviços padrão agora acompanham o idioma selecionado, inclusive nas notificações. Serviços personalizados e dados de manutenção são preservados.
- Mapa → Automático: ativa o modo escuro das 18h às 6h, usando o horário local do celular; Dia e Noite continuam disponíveis. A troca funciona durante o uso, sem reconectar a moto.
- Perspectiva em movimento no mapa do celular e na imagem enviada ao painel, com transição gradual para a vista superior ao parar. Ativa a partir de aproximadamente 7 km/h e desativa abaixo de aproximadamente 4 km/h, evitando oscilações.
- A perspectiva respeita o modo de acompanhamento e orientação do mapa. GPS antigo ou impreciso desativa a inclinação. A visão geral da rota permanece plana.
- No celular, a câmera também amplia o campo de visão em movimento. No painel, a perspectiva é aplicada ao mapa raster existente; não inclui prédios ou relevo 3D.

## Instalação e validação

Atualize em Configurações → Verificar atualização ou instale o APK por cima da versão anterior, sem desinstalar.

Compilação e testes automatizados verificam o horário automático e as regras de movimento. A aparência e a fluidez da nova perspectiva ainda precisam de validação no celular e na moto.
