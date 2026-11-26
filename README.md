# LinguaCoach — tutor AI per l'inglese (Android)

App Android in Kotlin/Compose per imparare l'inglese **scritto e parlato** con un tutor
AI che **corregge gli errori all'istante**. Ricostruzione originale del cuore didattico di
Praktika (analizzato con REA), senza il suo avatar 3D Unity/Rive né l'infrastruttura WebRTC.

## Funzioni
- **Conversazione** con un tutor AI (testo o voce), adattata al tuo livello CEFR (A1–C1).
- **Correzione istantanea**: ogni tuo messaggio riceve una scheda con gli errori
  (sbagliato → corretto + spiegazione in italiano) e un incoraggiamento.
- **Parlato**: riconoscimento vocale (SpeechRecognizer) per parlare, sintesi vocale
  (TextToSpeech) che legge le risposte in inglese.
- **Avatar personalizzabile** 2D: foto dalla galleria o preset, con animazione "parla"
  leggera (niente motore 3D).
- **Scenari** pronti (ristorante, colloquio, viaggio, medico…).

## Come funziona il "cervello"
Il tutor usa la **Messages API di Claude** (`POST /v1/messages`) via HTTPS diretto. L'app
chiede al modello di rispondere con un piccolo JSON `{reply, corrections[], encouragement}`,
così può mostrare la risposta naturale e le correzioni separate.

- Modello predefinito: **`claude-opus-5-5`** (selezionabile: Haiku 5.5 / Sonnet 5.5 / Opus 5.5
  nelle impostazioni). Per un tutor conversazionale **Haiku 5.5** è molto più economico e
  veloce, con ottima qualità sulla correzione: consigliato per l'uso quotidiano.
- La **chiave API resta sul dispositivo** (DataStore). È l'approccio tipico per un'app
  personale; per una distribuzione pubblica la chiave andrebbe dietro un piccolo backend
  proxy, non nell'app.

## Setup
1. Apri il progetto in **Android Studio** (o Gradle 8.9 + Android SDK 35) e sincronizza.
2. Serve una **chiave API Anthropic** (console.anthropic.com) — la inserisci all'avvio.
3. Concedi il permesso **Microfono** per parlare.

```bash
# da CLI, se hai Gradle + SDK:
cd linguacoach
gradle wrapper        # genera gradlew + gradle-wrapper.jar (non incluso: binario)
./gradlew assembleDebug
# APK in app/build/outputs/apk/debug/app-debug.apk
```

## Struttura
```
app/src/main/java/com/linguacoach/app/
  MainActivity.kt            onboarding vs chat
  AppViewModel.kt            orchestrazione (invio, STT/TTS, stato)
  ai/ClaudeClient.kt         chiamata Messages API + parsing JSON
  speech/SpeechManager.kt    riconoscimento vocale (parlato → testo)
  speech/Speaker.kt          sintesi vocale + stato "parla" per l'avatar
  data/                      Models, ChatRepository (DataStore)
  ui/                        Onboarding, Chat, AvatarView (animato), theme
```

> Nota: è una build **debug**. La correzione dipende dalla qualità del modello scelto.
