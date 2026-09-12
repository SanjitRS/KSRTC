# Smaran: Dual-Ecosystem Dementia Care & Cognitive Monitoring Platform

Smaran is a dual-application platform designed to empower Alzheimer's and dementia patients while providing caregivers with real-time safety, wandering detection, and clinical cognitive progression metrics.

---

## 📱 Pre-Built Application APKs

Direct pre-compiled installable APKs are available in the repository root and `release/` directory, as well as fast cloud mirrors:

| App | Description | Cloud Direct Link | GitHub Direct Link | Repository Link |
| :--- | :--- | :--- | :--- | :--- |
| **Smaran Patient App (Tracker)** | Patient companion with dynamic biological age configuration, 4 brain exercise games, clinical CPS calculation, continuous location & game session telemetry | [Download Patient APK (Catbox)](https://files.catbox.moe/95gxxp.apk) | [Download Patient APK (GitHub)](https://github.com/SanjitRS/KSRTC/raw/main/release/Smaran-Patient-App.apk) | [Smaran-Patient-App.apk](release/Smaran-Patient-App.apk) |
| **Smaran Caregiver App (Guardian)** | Real-time OpenStreetMap radar, safe zone geofencing, live clinical CPS dashboard with dynamic age sync, and live game session history log | [Download Caregiver APK (Catbox)](https://files.catbox.moe/ygixft.apk) | [Download Caregiver APK (GitHub)](https://github.com/SanjitRS/KSRTC/raw/main/release/Smaran-Caregiver-App.apk) | [Smaran-Caregiver-App.apk](release/Smaran-Caregiver-App.apk) |

---

## 🌟 Key Architecture & Capabilities

```
┌────────────────────────────────────────────────────────┐
│                   SMARAN ECOSYSTEM                     │
└────────────────────────────────────────────────────────┘
          ▲                                    ▲
          │                                    │
┌─────────────────────────┐          ┌─────────────────────────┐
│   SMARAN PATIENT APP    │          │  SMARAN CAREGIVER APP   │
│  (Companion & Tracker)  │          │   (Guardian Console)    │
├─────────────────────────┤          ├─────────────────────────┤
│ • 4 Brain Exercise Games│          │ • OpenStreetMap GPS     │
│ • Clinical CPS Engine   │ ◄──────► │ • Geofencing & Alerts   │
│ • Background GPS Beacon │   MQTT   │ • Live CPS Scoreboard   │
│ • Multilingual Voice TTS│  Relay   │ • Sub-Domain Analytics  │
│ • Cloud Auto-Broadcast  │          │ • Game Session History  │
└─────────────────────────┘          └─────────────────────────┘
```

### 1. Patient App (`assembleTrackerDebug`)
* **4 Clinical Brain Exercise Games**:
  - **Jumbo Memory Match**: Visual recall & spatial paired-association.
  - **Pattern Sequence Recall**: Working memory capacity & sequential processing.
  - **Color-Word Stroop Challenge**: Inhibitory response control & cognitive flexibility.
  - **Ascending Trail Making**: Visual search latency, psychomotor speed, and task-switching.
* **On-Device Clinical CPS Engine**:
  - Computes composite Cognitive Performance Score (0–100).
  - Evaluates sub-indices: Memory Retention, Executive Function, Latency, and Error Recovery.
  - Detects circadian rhythm / sundowning risks and fatigue trends.
* **Continuous Background Safety (`TrackerForegroundService`)**:
  - Low-battery GPS tracking beacon.
  - Instant retained MQTT broadcast upon completing any exercise.

### 2. Caregiver App (`assembleGuardianDebug`)
* **Tab 1: Interactive OpenStreetMap Radar & Safe Zones**:
  - Live patient marker with battery and timestamp indicators.
  - Adjustable circular geofence boundary (50m to 2,000m).
  - Wandering breach alarm and remote sound trigger.
* **Tab 2: Clinical CPS & Cognitive Health Analytics**:
  - Real-time Composite CPS gauge (0–100) & trajectory status (*Stable*, *Improving*, *Needs Attention*).
  - Functional Cognitive Age vs. Biological Age.
  - Clinical sub-domain breakdown progress bars.
  - On-demand "Sync Now" button.
* **Tab 3: Activity & Games Played History Log**:
  - Daily session counter.
  - Per-game breakdown: score, accuracy %, speed/latency (ms), rounds, and timestamp.

---

## 🛠️ Building From Source

### Prerequisites
- JDK 17+
- Android SDK (API 34)

### Build Commands

```bash
# Build Patient Companion App
./gradlew assembleTrackerDebug

# Build Caregiver Guardian App
./gradlew assembleGuardianDebug
```

Compiled APK locations:
* Patient App: `app/build/outputs/apk/tracker/debug/app-tracker-debug.apk`
* Caregiver App: `app/build/outputs/apk/guardian/debug/app-guardian-debug.apk`

---

## 📡 Cloud Relay & Communication
Both apps communicate seamlessly over an MQTT broker (`broker.emqx.io:1883`) using retained messaging and multi-channel cross-pairing fallback channels (`patient.device@smaran.local`, `guardian.device@smaran.local`, and `smaran_shared`).