# Atualizações do TerraNav pelo GitHub

Repositório configurado no aplicativo: https://github.com/ipsoltelecom/TerraNavapp

## Uso no celular

A versão 0.3 precisa ser instalada manualmente uma vez. A partir dela, ao abrir o app, o TerraNav consulta o último GitHub Release publicado, até uma vez por dia. Em caso de falha, uma nova verificação automática fica disponível após uma hora; o botão Verificar atualizações permite tentar antes. Não há tarefa de atualização rodando com o app fechado.

Quando houver uma versão mais nova, a tela Home mostra um aviso. Settings → Atualizações apresenta as novidades, o botão Baixar e depois Instalar. A instalação abre a confirmação do Android. Se o Android pedir autorização para instalar pelo TerraNav, permita, volte ao app e toque novamente em Instalar. O download não é retomado automaticamente se interrompido.

Verificação, download e instalação são bloqueados enquanto houver conexão com o painel ou rota de navegação ativa. Um download em andamento é interrompido se a navegação começar. A atualização exige o mesmo identificador e a mesma assinatura do aplicativo instalado e um versionCode maior. O arquivo também precisa corresponder ao SHA-256 e tamanho publicados.

## Publicar uma versão

1. Aumente versionCode e versionName em app/build.gradle.kts. Cada atualização deve ter um versionCode maior que todas as anteriores distribuídas.
2. Compile e teste com `:app:testDebugUnitTest :app:assembleDebug`. Use a mesma chave de assinatura das versões já instaladas. Não publique keystores, senhas nem arquivos google-services.json no repositório.
3. Gere o APK de distribuição e o manifesto com tools/prepare_release.py. O script lê o pacote real com aapt, calcula tamanho e SHA-256 e cria `terranav-update.json`.
4. Crie um GitHub Release com tag correspondente à versão, por exemplo `v0.3-test`, e descreva as novidades. Anexe os dois arquivos gerados: APK e `terranav-update.json`.
5. Publique como release normal e mais recente. Não marque **pre-release**, pois o atualizador consulta `/releases/latest`, que exclui pré-lançamentos e rascunhos. O sufixo `-test` no nome não impede uma release normal.
6. Confira os dois arquivos pela página pública. Uma tag Git sem um Release e seus arquivos não é suficiente.

O GitHub distribui os arquivos; novas versões ainda precisam ser compiladas, assinadas e publicadas. Não foi configurada compilação automática no GitHub Actions, pois ela exigiria configurar a chave de assinatura persistente como segredo do projeto. Builds com uma chave diferente serão recusadas pelo app.

## Verificação

Os testes UpdatePolicyTest cobrem comparação de versão, compatibilidade Android, origem dos downloads, redirecionamentos, hash obrigatório, limite de tamanho, identificador e igualdade das assinaturas. A instalação de uma atualização real deve ser validada no celular com duas versões assinadas pela mesma chave.

Fontes: https://docs.github.com/en/rest/releases/releases e https://developer.android.com/reference/android/content/pm/PackageInstaller
