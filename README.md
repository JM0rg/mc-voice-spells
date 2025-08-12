# YellSpells (Fabric 1.21.8)

Client-side, low-latency voice spellcasting that integrates cleanly with Simple Voice Chat (SVC) and the Magic System mod. Speech is transcribed locally via whisper.cpp (JNI), then mapped to commands like `/cast fireball`.

## Requirements

- Minecraft 1.21.8
- Fabric Loader ≥ 0.17.2
- Fabric API ≥ 0.131.0
- Simple Voice Chat (recommended)
- Java 21

## Highlights

- On-device STT (whisper.cpp) — no external services
- Low-latency partials, stability gating, and cooldowns
- Secure signed intents (HMAC) with server-side validation
- Data-driven voice mapping: multiple keywords, per-spell thresholds, target validation
- Works alongside SVC without patching it

## How it works

1) Client captures audio (SVC) → resamples (48k→16k) → VAD segments speech
2) Segments are transcribed by whisper.cpp via JNI
3) `SpellDetector` matches transcript to configured keywords
4) `SpellManager` executes `/cast <spellId>` on the server (or a per-spell custom command)
5) Magic System mod validates mana/cooldowns and performs the spell

## Configuration — `config/yellspells.json`

Created on first run; reload at runtime with `/yellspells reload`.

### Audio
- `audioBufferSize` (ms, default 400)
- `vadThreshold` (default 0.3)
- `sampleRate` (default 16000)

### STT
- `modelName` (default "tiny.en")
- `confidenceThreshold` (default 0.7)
- `stabilityThreshold` consecutive partials (default 2)
- `maxPartialLength` characters (default 50)

### Spells mapping
Each entry under `spells` looks like:

```json
"fireball": {
  "keywords": ["fireball", "cast fireball"],
  "cooldown": 2000,
  "confidenceThreshold": 0.6,
  "requiresTarget": true,
  "enabled": true,
  "command": "cast fireball" // optional override; defaults to "cast <id>"
}
```

Notes:
- Multiple keywords supported (`keywords` array). Back-compat: older `keyword` is still read.
- Disable any mapping by setting `enabled: false`.
- Use `command` to override the default `/cast <id>` if needed.

## Usage

1) Install YellSpells, Magic System, Fabric API, and Simple Voice Chat
2) Ensure `magicsystem_spells.json` defines the spells you want to voice-cast
3) Speak the configured keyword (e.g., “fireball”) — YellSpells executes the mapped command
4) Tune confidence/cooldowns in `yellspells.json` as needed

## Project structure (key files)

```
src/main/java/com/yellspells/
├── YellSpellsMod.java                     # Init, config load, networking, commands
├── client/
│   ├── audio/                             # Resampler, VAD
│   └── stt/                               # Whisper JNI, model management, detector
├── network/                               # Packets + HMAC session key exchange
├── spells/                                # Server-side command execution wiring
└── config/                                # YellSpellsConfig (JSON IO)
```

## Key components

- `AudioProcessor` — resampling + VAD; pushes blocks to STT worker
- `SpeechToTextManager` — whisper.cpp JNI interface and partial polling
- `SpellDetector` — stability-checked keyword matching (multi-keyword aware)
- `SpellManager` — validates cooldowns and executes commands (with override support)
- `YellSpellsNetworking` — per-session key exchange + HMAC signing/verification

## Performance

- Defaults to `tiny.en` model for low latency
- Background threads for STT; direct buffers to minimize GC
- VAD logs are quiet unless speaking; STT logs reduced when idle

## Troubleshooting

- No casts detected: verify mic permissions and SVC is running; lower `confidenceThreshold`
- False triggers: raise `confidenceThreshold` or increase `stabilityThreshold`
- High latency: reduce `audioBufferSize`; prefer `tiny.en`
- Server rejects: ensure Magic System is installed server-side and spell ids exist

## Security

- HMAC-signed intents using per-session keys
- Optional server-side raycast validation and rate limiting

## Building

```bash
./gradlew build
```

## License

MIT License (see `LICENSE`).

## Contributing

Issues and PRs welcome. Include versions and reproduction steps.
