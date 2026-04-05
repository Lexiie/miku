# Miku 🤖📱

**Your Personal AI Assistant for Android Automation**

Miku turns natural language into native Android actions. No menu-diving, no brittle macro flows, no fake assistant vibes. You say what you want, Miku translates it into structured actions, and your device executes them through real Android APIs.

Built on ElizaOS with a native Kotlin client, Miku combines conversational UX with local device control. The agent handles intent parsing and response generation, while the Android app performs the sensitive work on-device where it belongs.

## Why Miku Feels Different

**Native, not simulated**
Miku does not stop at chat responses. It is designed to drive real Android capabilities such as alarms, WiFi, SMS, reminders, brightness, and app launching.

**Cloud intelligence, local execution**
The agent can run remotely on Nosana, but execution still happens on the phone. That keeps the architecture flexible without turning private device actions into server-side behavior.

**Fast to try, clear to extend**
The repo is intentionally split into a small ElizaOS agent and a native Android app, so it is easy to understand, customize, and ship.

## What You Can Ask It To Do

Miku is built for commands such as:
- "Set alarm for 7 AM tomorrow"
- "Turn on WiFi"
- "Send SMS to 081234567890 saying I'm on my way"
- "Open Spotify"
- "Remind me to stretch in 30 minutes"

Core capability groups:
- Time management: alarms, timers, reminders, calendar events, agenda lookup
- Communication: SMS, phone calls, notifications
- Device controls: WiFi, Bluetooth, flashlight, brightness, volume, ringer mode
- Context and apps: open app, uninstall app, location lookup

## How It Works

```text
User message in Android app
        |
        v
POST /api/chat to ElizaOS agent
        |
        v
Parser returns structured actions
        |
        v
Android app executes each action locally
```

The project has two moving parts:
- a lightweight ElizaOS agent that exposes `/api/chat` and health endpoints
- an Android client that connects to the agent and executes the returned actions locally

Example response shape:

```json
{
  "text": "Alarm ready for 07:00. WiFi will be turned on",
  "actions": [
    {
      "type": "SET_ALARM",
      "params": {
        "hour": 7,
        "minute": 0,
        "label": "Alarm"
      }
    },
    {
      "type": "TOGGLE_WIFI",
      "params": {
        "enable": true
      }
    }
  ]
}
```

This keeps inference and command parsing on the agent side, while privacy-sensitive execution stays on the phone.

## Repository Layout

```text
miku/
├── src/                       # ElizaOS agent routes and parser
├── android/                   # Native Android app
├── characters/                # Character configuration
├── nos_job_def/               # Nosana job definition template
├── .github/workflows/         # CI/CD workflow
├── Dockerfile                 # Agent container image
├── package.json               # Node/ElizaOS dependencies
└── README.md
```

Important paths:
- `src/parser.ts`: natural-language to action parsing
- `src/api.ts`: `/api/chat`, `/health`, `/api/health`
- `android/app/src/main/java/com/miku/AutomationExecutor.kt`: Android action execution
- `characters/android.character.json`: active character file
- `.github/workflows/build-deploy.yml`: Android build plus Nosana deploy pipeline

## CI/CD Flow

On push to `main`, GitHub Actions currently:
- builds the Android debug APK
- uploads the APK as a workflow artifact
- builds and pushes the agent Docker image
- creates and starts a Nosana deployment through the Nosana HTTP API

Required GitHub secrets:

| Secret | Purpose |
| --- | --- |
| `DOCKER_USERNAME` | Docker Hub namespace for `DOCKER_IMAGE` |
| `DOCKER_PASSWORD` | Docker Hub access token |
| `NOSANA_API_KEY` | Nosana deployment API access |
| `GEMINI_API_KEY` | Gemini key for the agent runtime |

The workflow automatically tags the image as `${DOCKER_USERNAME}/miku` and injects Gemini settings into the deployment payload.

## Quick Start

### 1. Prepare secrets

Add the required repository secrets in GitHub Actions:
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`
- `NOSANA_API_KEY`
- `GEMINI_API_KEY`

### 2. Trigger the pipeline

```bash
git add .
git commit -m "Update Miku"
git push origin main
```

### 3. Get the outputs

After the workflow finishes:
- download the Android APK from the workflow artifacts
- open the Nosana dashboard to inspect the deployment
- copy the exposed deployment URL

### 4. Connect from Android

1. Install the APK on your device.
2. Open the Miku app.
3. Paste the deployed agent URL.
4. Tap `Connect`.

The app checks `/health` before using `/api/chat`.

## Local Development

### Agent

Install dependencies:

```bash
pnpm install
```

Create a local env file:

```bash
cp .env.example .env
```

Run the agent in development mode:

```bash
pnpm dev
```

Run the packaged agent:

```bash
pnpm start
```

Default local endpoint:

```text
http://localhost:3000
```

`.env.example` includes two common setups:
- Gemini via the OpenAI-compatible endpoint
- optional local Ollama development

### Android app

```bash
cd android
chmod +x gradlew
./gradlew assembleDebug
```

Useful commands:

```bash
./gradlew installDebug
./gradlew assembleRelease
```

APK outputs are written under:

```text
android/app/build/outputs/apk/
```

## API Surface

### Health

```http
GET /health
GET /api/health
```

### Chat

```http
POST /api/chat
Content-Type: application/json
```

Request body:

```json
{
  "text": "Set alarm for 7 AM and turn on WiFi",
  "userId": "android_user"
}
```

The response always contains:
- `text`: user-facing summary
- `actions`: structured action list for the Android executor

## Android Execution Model

The Android app uses Jetpack Compose and a simple MVVM structure:
- `ChatViewModel` handles connection state, messages, and API calls
- `ApiClient` normalizes URLs and checks health endpoints
- `AutomationExecutor` maps action types to Android APIs
- `ReminderReceiver` handles reminder notifications

Runtime permissions are requested only when the related action needs them.

## Customization

### Tune parser behavior

Update `src/parser.ts` when you want to add or refine supported commands.

### Tune character behavior

Update `characters/android.character.json` when you want to adjust examples, system prompt, or model settings.

### Add a new Android action

1. Add the action type and parsing logic in `src/parser.ts`.
2. Handle the new action in `AutomationExecutor.kt`.
3. Update character examples if the new action needs stronger prompting.

## Troubleshooting

| Issue | What to check |
| --- | --- |
| App cannot connect | Confirm the URL is reachable and includes the correct protocol (`https://` for deployed endpoints). |
| Health check fails | Verify the agent is serving `/health` or `/api/health`. |
| Action does not execute | Check Android runtime permissions and inspect Logcat output. |
| SMS or call actions fail | Confirm the device granted telephony permissions. |
| Local Android build fails | Use JDK 17 and rebuild with `./gradlew --stacktrace assembleDebug`. |
| Nosana deployment fails | Inspect the latest GitHub Actions run and the Nosana dashboard response. |

## Tech Stack

- ElizaOS for the agent runtime
- TypeScript for parsing and API routes
- Kotlin + Jetpack Compose for the Android client
- Retrofit + OkHttp for Android networking
- Nosana for deployment
- Gemini models through the OpenAI-compatible Gemini endpoint

## License

MIT. See `LICENSE`.
