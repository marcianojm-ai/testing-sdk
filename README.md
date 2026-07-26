🚀 Monitor de Testes SDK
Um SDK simples e direto ao ponto, criado especialmente para ajudar desenvolvedores a monitorar os testes de aplicativos exigidos pelo Google Play Console.

Integração fácil e rápida, pensada para quem está começando no Android Studio!

⚙️ Pré-requisitos
Android Studio (versões recentes com suporte a Kotlin DSL build.gradle.kts)

Minimum SDK (API Level): 23 (Android 6.0) ou superior

📦 Como instalar
A instalação leva menos de 2 minutos. Siga os dois passos abaixo:

Passo 1: Adicionar o repositório JitPack
No seu Android Studio, abra o arquivo settings.gradle.kts (ele fica na raiz do seu projeto).
Adicione a linha do JitPack dentro do bloco dependencyResolutionManagement, logo abaixo de mavenCentral():

Kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // <-- Adicione APENAS esta linha
    }
}
Passo 2: Adicionar a dependência do SDK
Agora, abra o arquivo build.gradle.kts do seu aplicativo (atenção: é o arquivo que tem escrito Module: app).
Role até o final do arquivo e adicione o nosso SDK dentro do bloco dependencies:

Kotlin
dependencies {
    // Suas outras dependências estarão aqui...
    
    // Adicione o SDK de Monitoramento
    implementation("com.github.marcianojm-ai:testing-sdk:1.0.0")
}
Após colar as duas linhas, clique no botão Sync Now (Sincronizar Agora) que vai aparecer no topo direito do seu Android Studio.

💻 Como usar no código
Após o Gradle sincronizar com sucesso, o SDK já está pronto para uso no seu aplicativo.

Abra a sua MainActivity e inicie o monitoramento chamando o código abaixo dentro do método onCreate:

Kotlin
import com.suaempresa.testing.MonitorDeTestes // O Android Studio vai sugerir o import automático

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicia o monitoramento do Google
        MonitorDeTestes.iniciar(this, "SEU_CODIGO_DE_PROJETO_AQUI")
    }
}
(Nota: Substitua "SEU_CODIGO_DE_PROJETO_AQUI" pelo código fornecido no nosso painel web).

🆘 Precisa de ajuda?
Se você encontrou algum problema durante a instalação ou tem dúvidas sobre como os testes funcionam, acesse nosso site de suporte:
👉 [Link para o seu site / painel aqui]
