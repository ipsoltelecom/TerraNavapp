# TerraNav

Aplicativo Android de navegação para uso com o painel Tripper Dash da Royal Enfield Himalayan 450. O usuário validou a versão 0.3 com sua moto. A versão 0.5 e seus novos perfis ainda precisam de validação física.

## Versão 0.5-test

- Idioma selecionável e persistente: inglês ou português brasileiro.
- Qualidade de transmissão Econômica, Equilibrada ou Alta, aplicada na próxima conexão. Equilibrada preserva o perfil validado.
- Interface azul-escuro/preta e identidade TerraNav.
- Mapa Dia/Noite/Automático (escuro das 18h às 6h locais), no celular e no painel.
- Perspectiva gradual em movimento, com campo de visão ampliado e retorno à vista superior ao parar.
- Serviços padrão da Garagem traduzidos conforme o idioma selecionado.
- Camada experimental de semáforos, radares fixos e limites cadastrados no OpenStreetMap. As consultas ao Overpass não foram validadas nesta rede por falhas/timeouts; cobertura e disponibilidade variam.
- Atualizações por GitHub Releases: verificação ao abrir, novidades, download verificado e instalação confirmada pelo Android. Quem já tem a versão 0.3 pode atualizar pelas Configurações do app.
- Sem acidentes, obras ou trânsito em tempo real nesta etapa.

Baixe o APK em [Releases](https://github.com/ipsoltelecom/TerraNavapp/releases). As regras de publicação e assinatura estão em [DISTRIBUICAO.md](DISTRIBUICAO.md).

## Compilar

Abra a pasta no Android Studio, configure o SDK local e execute `:app:testDebugUnitTest :app:assembleDebug`. O projeto usa Java 21 para Gradle, Gradle 9.4.1, AGP 9.2.1, compileSdk 36.1 e minSdk 24. Firebase é opcional; sem google-services.json o app opera localmente.

O identificador de instalação é `br.tec.ipsol.terranav.test`. Preserve a chave de assinatura de quem distribui as versões para que atualizações sejam aceitas. A chave não está neste repositório; uma compilação em outro computador terá uma chave diferente por padrão.

## Origem e dados

Derivado de [RE_TFTDash](https://github.com/sxdpz23/RE_TFTDash), commit `49d88a83a666adab78c23fb19526aea7bb1b18d9`. A licença MIT e os créditos originais estão em [LICENSES/Northstar-MIT.txt](LICENSES/Northstar-MIT.txt). O arquivo LICENSE preexistente deste repositório foi preservado.

Dados viários © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), ODbL. Mapa vetorial do celular via [OpenFreeMap](https://openfreemap.org/). O renderizador do painel mantém o provedor raster herdado do projeto-base, com filtro noturno local. Não é espelhamento do aplicativo Google Maps.
