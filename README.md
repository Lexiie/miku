# Miku 🤖📱

**Your Personal AI Assistant for Android Automation**

Miku transforms natural language into native Android actions. No more tapping through menus—just tell Miku what you want, and it happens instantly.

Built on ElizaOS with decentralized inference, Miku bridges conversational AI with deep Android system integration, giving you voice-controlled automation that runs on your terms.

---

## 🎯 What Makes Miku Different

**True Native Integration** — Unlike chatbots that just respond with text, Miku executes real Android API calls. Set alarms, toggle WiFi, send SMS, control brightness—all through natural language.

**Decentralized Intelligence** — Your agent runs on Nosana's distributed GPU network, not centralized cloud providers. You control the infrastructure, you own the data.

**Zero Configuration** — No API keys, no complex setup. Just deploy the agent, install the app, connect, and start automating.

**Hybrid Architecture** — ElizaOS handles the intelligence layer (intent parsing, parameter extraction), while native Android APIs handle execution. Best of both worlds.

---

## ✨ Capabilities

### ⏰ Time Management
- **Set Alarm** — "Set alarm for 7 AM tomorrow"
- **Set Timer** — "Timer for 10 minutes"
- **Calendar Events** — "Add meeting with John at 2 PM"
- **View Schedule** — "What's on my calendar today?"
- **Reminders** — "Remind me to call mom in 30 minutes"

### 📱 Communication
- **Send SMS** — "Text 081234567890 saying I'm running late"
- **Make Calls** — "Call 081234567890"
- **Notifications** — "Notify me to take a break"

### 🔧 System Control
- **WiFi** — "Turn on WiFi" / "Disable WiFi"
- **Bluetooth** — "Enable Bluetooth"
- **Flashlight** — "Turn on flashlight"
- **Brightness** — "Set brightness to 80%"
- **Volume** — "Set volume to 50%"
- **Ringer Mode** — "Set phone to silent" / "Vibrate mode"

### 📍 Location & Apps
- **Get Location** — "Where am I?"
- **Open Apps** — "Open Spotify"
- **Uninstall Apps** — "Uninstall Twitter"

---

## 🏗️ Architecture

Miku uses a **hybrid client-server architecture** where intelligence lives in the cloud and execution happens locally:

```
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID DEVICE                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  User Input (Text/Voice)                             │  │
│  │  "Set alarm for 7 AM"                                │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              │ HTTP POST                     │
│                              ▼                               │
└──────────────────────────────┼──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              NOSANA DECENTRALIZED COMPUTE                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ElizaOS Agent + Qwen3.5-27B                         │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │ Intent Parser                                   │ │  │
│  │  │ • Extract action type (SET_ALARM)              │ │  │
│  │  │ • Extract parameters (hour: 7, minute: 0)      │ │  │
│  │  │ • Generate structured JSON                     │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              │ JSON Response                 │
│                              ▼                               │
└──────────────────────────────┼──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID DEVICE                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  AutomationExecutor                                  │  │
│  │  {                                                   │  │
│  │    "type": "SET_ALARM",                             │  │
│  │    "params": {"hour": 7, "minute": 0}               │  │
│  │  }                                                   │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              │                               │
│                              ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  AlarmManager.setExactAlarm(...)                     │  │
│  │  ✅ Native Android API Executed                      │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**Why This Architecture?**

- **Separation of Concerns** — AI inference happens on powerful GPUs, execution happens locally
- **Privacy** — Sensitive actions (SMS, calls) never leave your device
- **Scalability** — Agent can serve multiple devices simultaneously
- **Flexibility** — Swap models, update logic, without touching the Android app

---

## 🚀 Quick Start

### Prerequisites

- **Docker Hub account** (free)
- **GitHub account** with Actions enabled
- **Nosana API key** from [deploy.nosana.com](https://deploy.nosana.com/account/)
- **Android device** (API 26+)

### Step 1: Configure Deployment

**1. Setup GitHub Secrets**

Go to your repo → Settings → Secrets and variables → Actions, add:

| Secret | Value | Where to Get |
|--------|-------|--------------|
| `DOCKER_USERNAME` | Your Docker Hub username | [hub.docker.com](https://hub.docker.com) |
| `DOCKER_PASSWORD` | Docker Hub access token | [hub.docker.com/settings/security](https://hub.docker.com/settings/security) |
| `NOSANA_API_KEY` | Nosana API key | [deploy.nosana.com/account](https://deploy.nosana.com/account/) |

**2. Update Docker Image Name**

Edit `.github/workflows/build-deploy.yml`:
```yaml
env:
  DOCKER_IMAGE: YOUR_DOCKERHUB_USERNAME/miku
```

Edit `nos_job_def/nosana_eliza_job_definition.json`:
```json
{
  "args": {
    "image": "YOUR_DOCKERHUB_USERNAME/miku:latest",
    ...
  }
}
```

### Step 2: Deploy

```bash
git add .
git commit -m "Deploy Miku"
git push origin main
```

GitHub Actions will automatically:
1. ✅ Build Android APK
2. ✅ Build Docker image
3. ✅ Push to Docker Hub
4. ✅ Deploy to Nosana
5. ✅ Create GitHub Release with APK

### Step 3: Install & Connect

1. **Download APK** from [Releases page](../../releases/latest)
2. **Install** on your Android device
3. **Get agent URL** from Nosana dashboard (format: `https://xxx.node.k8s.prd.nos.ci`)
4. **Open Miku app** → Enter URL → Tap "Connect"
5. **Start automating!**

---

## 🛠️ Technical Deep Dive

### Intent Parsing Engine

Miku uses a custom ElizaOS action handler that parses natural language into structured JSON:

```typescript
// Input: "Set alarm for 7 AM tomorrow"
// Output:
{
  "text": "⏰ Alarm set for 7:00 AM",
  "actions": [{
    "type": "SET_ALARM",
    "params": {
      "hour": 7,
      "minute": 0,
      "label": "Alarm"
    }
  }]
}
```

The parser handles:
- **Time extraction** — Relative ("in 10 minutes") and absolute ("7 AM")
- **Contact resolution** — Names to phone numbers
- **Parameter inference** — Smart defaults when info is missing
- **Multi-action commands** — "Turn on WiFi and set brightness to 50%"

### Android Execution Layer

`AutomationExecutor.kt` maps action types to native Android APIs:

| Action Type | Android API | Permission Required |
|-------------|-------------|---------------------|
| `SET_ALARM` | `AlarmManager.setExactAlarm()` | `SCHEDULE_EXACT_ALARM` |
| `SEND_SMS` | `SmsManager.sendTextMessage()` | `SEND_SMS` |
| `TOGGLE_WIFI` | `WifiManager.setWifiEnabled()` | `CHANGE_WIFI_STATE` |
| `SET_BRIGHTNESS` | `Settings.System.putInt()` | `WRITE_SETTINGS` |
| `GET_LOCATION` | `FusedLocationProviderClient` | `ACCESS_FINE_LOCATION` |
| `TOGGLE_FLASHLIGHT` | `CameraManager.setTorchMode()` | `CAMERA` |

**Permission Handling** — Miku requests permissions just-in-time. When you first send SMS, it asks for SMS permission. When you first set brightness, it opens system settings.

### Communication Protocol

**Request:**
```json
POST /api/chat
{
  "text": "Set alarm for 7 AM",
  "userId": "android_user"
}
```

**Response:**
```json
{
  "text": "⏰ Alarm set for 7:00 AM",
  "actions": [
    {
      "type": "SET_ALARM",
      "params": {
        "hour": 7,
        "minute": 0,
        "label": "Alarm"
      }
    }
  ]
}
```

The Android app executes each action sequentially and displays results in the chat.

### State Management

Miku uses **Jetpack Compose + ViewModel** for reactive UI:

- `ChatViewModel` — Manages messages, connection state, API calls
- `AutomationExecutor` — Stateless executor for Android APIs
- `ApiClient` — Retrofit HTTP client with automatic retry

No local storage needed—all state is ephemeral. Privacy by design.

---

## 📦 Project Structure

```
miku/
├── src/                                    # ElizaOS Agent
│   ├── actions/
│   │   └── androidAutomation.ts           # Intent parser (17 actions)
│   ├── api.ts                             # REST API endpoint
│   └── index.ts                           # Plugin entry point
│
├── android/                                # Android App
│   ├── app/
│   │   ├── build.gradle.kts               # Build config
│   │   └── src/main/
│   │       ├── AndroidManifest.xml        # Permissions & config
│   │       ├── java/com/miku/agent/
│   │       │   ├── MainActivity.kt        # Compose UI
│   │       │   ├── ChatViewModel.kt       # State management
│   │       │   ├── ApiClient.kt           # HTTP client
│   │       │   ├── AutomationExecutor.kt  # Android API executor
│   │       │   └── Models.kt              # Data classes
│   │       └── res/
│   │           ├── values/strings.xml
│   │           └── values/themes.xml
│   ├── build.gradle.kts                   # Root build file
│   ├── settings.gradle.kts                # Project settings
│   └── gradlew                            # Gradle wrapper
│
├── characters/
│   └── android.character.json             # Agent personality & examples
│
├── nos_job_def/
│   └── nosana_eliza_job_definition.json   # Nosana deployment config
│
├── .github/workflows/
│   └── build-deploy.yml                   # CI/CD pipeline
│
├── Dockerfile                             # Container config
├── package.json                           # Node dependencies
└── README.md                              # This file
```

---

## 🔧 Development

### Local Agent Development

```bash
# Install dependencies
pnpm install

# Run with Nosana endpoint (production)
pnpm start

# Or run locally with Ollama (development)
ollama pull qwen3.5:27b
ollama serve

# Update .env
OPENAI_API_KEY=ollama
OPENAI_API_URL=http://127.0.0.1:11434/v1
MODEL_NAME=qwen3.5:27b

# Start agent
pnpm dev
```

Agent runs on `http://localhost:3000`

### Android App Development

```bash
cd android

# Debug build
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Release build
./gradlew assembleRelease
```

APK output: `android/app/build/outputs/apk/`

### Testing Locally

1. Get your computer's local IP: `ifconfig` (Linux/Mac) or `ipconfig` (Windows)
2. Run agent locally: `pnpm dev`
3. In Miku app, enter: `http://YOUR_LOCAL_IP:3000`
4. Ensure phone and computer are on same WiFi network

---

## 🚢 Deployment

### Automated Deployment (Recommended)

GitHub Actions handles everything automatically:

**On every push to `main`:**
1. Builds Android APK
2. Builds Docker image
3. Pushes to Docker Hub
4. Deploys to Nosana
5. Creates GitHub Release with APK

**Setup:**
1. Configure GitHub Secrets (see Quick Start)
2. Update Docker image names
3. Push to `main`

### Manual Deployment

**Build Docker Image:**
```bash
docker build -t yourusername/miku:latest .
docker push yourusername/miku:latest
```

**Deploy to Nosana:**
```bash
npm install -g @nosana/cli

nosana job post \
  --file ./nos_job_def/nosana_eliza_job_definition.json \
  --market nvidia-3090 \
  --timeout 300 \
  --api YOUR_NOSANA_API_KEY
```

**Monitor Deployment:**
```bash
# Check status
nosana job status <job-id>

# View logs
nosana job logs <job-id>
```

---

## 📱 Android App Features

### Chat Interface
- **Material Design 3** — Modern, clean UI with dynamic theming
- **Real-time messaging** — Instant feedback on action execution
- **Connection indicator** — Visual status (green = connected, red = disconnected)
- **Auto-scroll** — Always shows latest messages

### Dynamic Endpoint Configuration
- **Manual URL input** — Connect to any ElizaOS agent
- **Persistent connection** — Maintains session across app lifecycle
- **Error handling** — Graceful fallback on network issues

### Permission Management
- **Just-in-time requests** — Only asks when needed
- **Clear explanations** — Shows why each permission is required
- **Graceful degradation** — Continues working even if some permissions denied

### Execution Feedback
Every action shows:
- ✅ Success confirmation
- ⚡ Action type executed
- 📊 Result details (when applicable)

---

## 🔐 Security & Privacy

**Privacy-First Design:**
- No data collection or analytics
- No cloud storage of messages
- All sensitive actions (SMS, calls) execute locally
- Agent only receives command text, not contact lists or personal data

**Permission Model:**
- Runtime permissions requested on-demand
- User has full control over what Miku can access
- Permissions can be revoked anytime via Android settings

**Network Security:**
- HTTPS required for production deployments
- Cleartext traffic only allowed for local development
- No authentication tokens stored on device

---

## 🧪 Testing

### Test Commands

Try these to verify all features work:

```
⏰ Time Management:
- "Set alarm for 7 AM"
- "Set timer 5 minutes"
- "Add meeting tomorrow at 2 PM"

🔧 System Control:
- "Turn on WiFi"
- "Set brightness to 70%"
- "Turn on flashlight"
- "Set phone to silent"

📱 Communication:
- "Send SMS to 081234567890 saying hello"
- "Notify me to take a break"

📍 Location:
- "Where am I?"

📱 Apps:
- "Open Chrome"
```

### Troubleshooting

| Issue | Solution |
|-------|----------|
| **Can't connect to agent** | Verify URL format: `https://xxx.node.k8s.prd.nos.ci` (include https://) |
| **Permission denied** | Grant permission when prompted, or check Settings → Apps → Miku → Permissions |
| **Action not executing** | Check Logcat for errors: `adb logcat \| grep Miku` |
| **Agent not responding** | Check Nosana dashboard for job status and logs |
| **Build fails** | Ensure JDK 17 installed: `java -version` |
| **Gradle sync fails** | Delete `.gradle` folder and sync again |

---

## 🎨 Customization

### Modify Agent Behavior

Edit `characters/android.character.json`:

```json
{
  "name": "Miku",
  "system": "Your custom instructions here...",
  "messageExamples": [
    // Add more examples to improve parsing accuracy
  ]
}
```

### Add New Actions

**1. Add to Intent Parser** (`src/actions/androidAutomation.ts`):
```typescript
else if (userText.includes("screenshot")) {
  response.actions.push({
    type: "TAKE_SCREENSHOT",
    params: {}
  });
  response.text = "📸 Screenshot taken";
}
```

**2. Add to Executor** (`android/.../AutomationExecutor.kt`):
```kotlin
"TAKE_SCREENSHOT" -> takeScreenshot(action.params)
```

**3. Implement Android API**:
```kotlin
private fun takeScreenshot(params: Map<String, Any>): String {
    // Your implementation
    return "✅ Screenshot saved"
}
```

### Extend with ElizaOS Plugins

Add more capabilities:

```bash
pnpm add @elizaos/plugin-web-search
```

Update `characters/android.character.json`:
```json
{
  "plugins": [
    "@elizaos/plugin-bootstrap",
    "@elizaos/plugin-openai",
    "@elizaos/plugin-web-search"
  ]
}
```

---

## 🏗️ Tech Stack

### Backend (ElizaOS Agent)
- **Framework:** ElizaOS v2
- **Runtime:** Node.js 23
- **LLM:** Qwen3.5-27B (27B parameters, AWQ 4-bit quantization)
- **Inference:** Nosana decentralized GPU network
- **API:** Express.js REST endpoint
- **Container:** Docker

### Frontend (Android App)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Architecture:** MVVM (ViewModel + State)
- **HTTP Client:** Retrofit + OkHttp
- **Async:** Kotlin Coroutines
- **Location:** Google Play Services FusedLocationProvider
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)

### Infrastructure
- **Compute:** Nosana decentralized network
- **CI/CD:** GitHub Actions
- **Container Registry:** Docker Hub
- **Deployment:** Automated via GitHub workflow

---

## 📊 Performance

**Agent Response Time:**
- Intent parsing: ~500ms
- Total round-trip: <2s (including network)

**Android Execution:**
- Action execution: <100ms (native APIs)
- UI update: Instant (Compose reactivity)

**Resource Usage:**
- APK size: ~8MB
- Memory footprint: ~50MB
- Battery impact: Minimal (no background services)

---

## 🤝 Contributing

Contributions welcome! Areas for improvement:

- [ ] Voice input integration (SpeechRecognizer)
- [ ] Multi-step action sequences
- [ ] Action history & undo
- [ ] Widget support
- [ ] Tasker integration
- [ ] More Android APIs (camera, media, sensors)

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file

---

## 🙏 Acknowledgments

Built with:
- [ElizaOS](https://elizaos.com) - AI agent framework
- [Nosana](https://nosana.com) - Decentralized compute
- [Qwen](https://huggingface.co/Qwen) - Open-source LLM

---

**Miku** — Your device, your assistant, your control. 🤖✨
