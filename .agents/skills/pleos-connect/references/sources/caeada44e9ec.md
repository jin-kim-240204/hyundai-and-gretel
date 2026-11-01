# Source: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk
Retrieved: 2026-11-01

Pleos Connect 개발자 가이드
앱 개발·연동 가이드
SDK 사용 가이드
TextToSpeech SDK
Programming Guide
Importing TextToSpeech SDK
Importing TextToSpeech SDK
TextToSpeech SDK를 사용하려면 Pleos Maven 저장소를 등록하고 앱 모듈에 SDK 의존성을 추가합니다.

Maven 저장소 등록​

프로젝트의 settings.gradle.kts 또는 settings.gradle 파일에 Pleos Maven 저장소를 추가합니다.

Kotlin DSL
Groovysettings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
    }
}


SDK 의존성 추가​

앱 모듈의 Gradle 파일에 TextToSpeech SDK 의존성을 추가합니다.

Kotlin DSL
Groovyapp/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
}
app/build.gradle
dependencies {
    implementation "ai.pleos.playground:TextToSpeech:3.3.1"
}


Gradle 동기화를 완료한 후  [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions] TextToSpeech 권한을 선언합니다.
