# YellSpells

A low-latency, client-side voice spellcasting mod for Minecraft 1.21.8 on Fabric that integrates with Simple Voice Chat (SVC) without forking it. YellSpells performs on-device speech-to-text using whisper.cpp via JNI and enables voice-activated spellcasting.

## Features

- **Voice-Activated Spells**: Cast spells by speaking their keywords
- **On-Device STT**: All speech processing happens locally using whisper.cpp
- **Low Latency**: Sub-second partials and ~0.7–1.2s trigger from speech end
- **Anti-Cheat**: Signed cast-intents with HMAC verification and server-side validation
- **Configurable Spells**: Easy spell configuration with cooldowns and confidence thresholds
- **Clean Integration**: Works alongside SVC without modifying it

## Requirements

- Minecraft 1.21.8
- Fabric Loader 0.15.0+
- Fabric API 0.131.0+
- Simple Voice Chat 2.5.0+ (suggested dependency)
- Java 17+

## Installation

1. Install Fabric Loader for Minecraft 1.21.8
2. Download and install Fabric API
3. Download and install Simple Voice Chat
4. Download YellSpells and place it in your mods folder
5. Start Minecraft and configure the mod

## Configuration

The mod creates a configuration file at `config/yellspells.json` with the following settings:

### Audio Processing
- `audioBufferSize`: Audio buffer size in milliseconds (default: 400ms)
- `vadThreshold`: Voice activity detection threshold (default: 0.3)
- `sampleRate`: Audio sample rate (default: 16000)

### Speech Recognition
- `modelName`: Whisper model to use (default: "tiny.en")
- `confidenceThreshold`: Minimum confidence for spell detection (default: 0.7)
- `stabilityThreshold`: Consecutive detections needed for stability (default: 2)
- `maxPartialLength`: Maximum partial transcript length (default: 50)

### Spells
Each spell can be configured with:
- `keyword`: The word to speak to cast the spell
- `cooldown`: Cooldown in milliseconds
- `confidenceThreshold`: Minimum confidence for this spell
- `requiresTarget`: Whether the spell requires a target

### Spell Integration
YellSpells now integrates with the Magic System mod for spell implementations:
- **Voice Detection**: YellSpells handles speech-to-text and keyword detection
- **Spell Execution**: Magic System mod handles actual spell casting via `/cast` commands
- **Seamless Integration**: Voice commands automatically trigger the appropriate spell commands

### Default Spells
- **Fireball**: Say "fireball" to execute `/cast fireball` command

## Usage

1. Install both YellSpells and Magic System mods
2. Join a server with both mods installed
3. Speak a spell keyword clearly into your microphone (e.g., "fireball")
4. YellSpells will detect the keyword and execute the corresponding `/cast` command
5. Check the HUD for transcript and confidence information

## Development

### Building from Source

```bash
./gradlew build
```

### Project Structure

- `src/main/java/com/yellspells/` - Main mod classes
- `src/main/java/com/yellspells/client/` - Client-side components
- `src/main/java/com/yellspells/network/` - Networking and packets
- `src/main/java/com/yellspells/spells/` - Spell command execution
- `src/main/java/com/yellspells/config/` - Configuration system

### Key Components

1. **SVCAudioIntegration**: Official SVC API integration for audio capture
2. **AudioProcessor**: Handles audio resampling (48kHz→16kHz) and VAD
3. **AudioResampler**: High-quality polyphase filter for audio conversion
4. **SpeechToTextManager**: Manages whisper.cpp JNI integration
5. **SpellDetector**: Detects spells from transcriptions with stability checking
6. **SpellManager**: Server-side command execution and validation
7. **YellSpellsNetworking**: Secure client-server communication with HMAC signing

### Adding New Spells

1. Create the spell implementation in the Magic System mod
2. Add the spell keyword to YellSpells configuration in `YellSpellsConfig`
3. The voice command will automatically execute `/cast <spellname>` when detected

## Security

- All cast-intents are signed with HMAC using per-session keys
- Server-side validation includes time skew checks and nonce verification
- Raycast validation ensures legitimate targeting
- Rate limiting prevents spam

## Performance

- Uses whisper.cpp's tiny.en model by default for optimal performance
- High-quality audio resampling (48kHz→16kHz) with polyphase filtering
- Direct buffer JNI interface for minimal GC overhead
- Audio processing runs in background threads
- Minimal impact on game performance
- Configurable for different hardware capabilities

## Troubleshooting

### Common Issues

1. **No audio detected**: Check microphone permissions and SVC settings
2. **Spells not casting**: Verify confidence threshold and keyword pronunciation
3. **High latency**: Try reducing audio buffer size or using tiny.en model
4. **Crashes**: Check for conflicts with other mods

### Logs

Check the Minecraft logs for detailed error information. Look for entries from "YellSpells" or "YellSpellsClient".

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Acknowledgments

- [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat) for voice chat functionality
- [whisper.cpp](https://github.com/ggerganov/whisper.cpp) for speech recognition
- [Fabric](https://fabricmc.net/) for the modding framework
