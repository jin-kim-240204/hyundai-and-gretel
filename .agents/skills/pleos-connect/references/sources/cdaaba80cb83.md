# Source: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro
Retrieved: 2026-11-01

Pleos Connect 개발자 가이드
앱 개발·연동 가이드
SDK 사용 가이드
TextToSpeech SDK
Introduction
Introduction
TextToSpeech SDK 소개​

TextToSpeech SDK는 텍스트를 음성으로 합성하고 재생하기 위한 API를 제공합니다. 3rd party 앱은 TextToSpeech SDK를 통해 Pleos Connect의 Speech Service와 연동됩니다. 3rd party 앱이 텍스트의 음성 합성을 요청하면 Speech Service가 이를 처리하고, 합성된 음성을 차량의 오디오 시스템으로 재생합니다.

SDK 버전
이 가이드는 TextToSpeech SDK v3.3.1을 기준으로 작성되었습니다.

음성 합성 모드​

TextToSpeech SDK는 다음과 같이 세 가지 음성 합성 모드를 제공합니다.


HYBRID: 기본 모드입니다. 서버 기반 streaming TextToSpeech를 우선 사용하고, 네트워크 연결에 실패하면 SDK가 On-device 엔진으로 자동 전환합니다. 서버 기반 합성을 사용하려면 Pleos Playground의 My Project 자격 증명을 사용한 앱 등록이 필요합니다.

SERVER: 서버 기반 streaming TextToSpeech만 사용합니다. 네트워크 연결과 앱 등록이 필요하며, 네트워크 연결에 실패하면 On-device 엔진으로 전환하지 않고 오류 이벤트를 전달합니다.

ON_DEVICE: 차량의 On-device 엔진에서 Streaming TextToSpeech를 실행합니다. 네트워크 연결이 필요하지 않으며, 서버 기반 합성을 사용하지 않으므로 앱 등록이 필요하지 않습니다.


진입점​

ai.pleos.playground.tts.TextToSpeech 클래스는 TextToSpeech SDK 기능에 접근하기 위한 entry point입니다. 생성자에 애플리케이션 Context와 실행 Mode를 전달하여 객체를 생성합니다. Mode를 생략하면 HYBRID가 적용됩니다.

일반적인 사용 순서는 다음과 같습니다.


TextToSpeech 객체를 생성합니다.

initialize()로 SDK를 초기화합니다.

addEventListener()로 이벤트 리스너를 등록합니다.

필요한 경우 setLocale()로 음성 합성 언어/지역을 설정합니다.

서버 기반 합성 모드(HYBRID, SERVER)를 사용하는 경우 앱 등록을 완료합니다.

서버 연결이 필요한 모드에서는 OnServerConnectionListener.onConnected() 이후 speak()를 호출합니다.

인식이 끝나면 stop()을 호출합니다.

SDK 사용이 끝나면 removeEventListener() 후 release()로 리소스를 해제합니다.


TextToSpeech 객체 생성 예시​

Kotlin
Java
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode

val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID)

import ai.pleos.playground.tts.TextToSpeech;
import ai.pleos.playground.tts.constant.Mode;

TextToSpeech textToSpeech =
        new TextToSpeech(getApplicationContext(), Mode.HYBRID);


Emulator setup​

TextToSpeech SDK를 사용하려면 Android Studio에 Pleos Connect Emulator를 설치하고 AVD를 설정합니다.

 [link: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator#소프트웨어-개발-환경] 소프트웨어 개발 환경 요구사항 확인
 [link: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator#installing-the-pleos-connect-emulator] Pleos Connect Emulator 설치
 [link: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator#setting-up-the-emulator] AVD 생성 및 실행
 [link: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator] Pleos Connect Virtual Console 설정
 [link: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator#마이크-접근-설정] 마이크 접근 설정
 [link: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator#cold-boot-설정] Cold boot 설정

다음 단계​


 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk] TextToSpeech SDK 가져오기

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions] 권한 선언

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release] SDK 초기화 및 해제

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners] 이벤트 리스너

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale] 로케일 설정

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration] 앱 등록

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop] 음성 합성 시작 및 중지

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/error-code] 오류 코드

 [link: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/entry] TextToSpeech API Reference

## Original diagrams and visual specifications
https://document.pleos.ai/assets/images/tts-sdk-e0a43b9f19b61628c27b2e506b38fc3f.png
