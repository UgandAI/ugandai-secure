# Voice hardware follow-up

When a physical Android device is available:

1. Install the same debug build and grant `RECORD_AUDIO` while using the app.
2. Capture 15 seconds of silence; confirm no `SEND_AUDIO` or `/voice/chat` request occurs.
3. Record “The quick brown fox jumps over the lazy dog” and confirm PCM RMS responds, the retained WAV is audible, and the STT text matches.
4. Record “What should I plant on my five acre farm this season?” and verify the same session ID and SHA-256 across capture, upload, STT, ViewModel, and `SEND` logs.
5. Test short utterances: “yes”, “no”, “maize”, and “why?”.
6. Exit/re-enter Voice Mode during capture and during STT; confirm stale-session logs appear and no old result becomes a chat message.
7. Compare the physical device’s routed input, RMS baseline, and clear-speech RMS with the emulator evidence before tuning the evidence threshold.

Useful log filter:

```shell
adb logcat -v time -s UgandAIVoiceCapture:D UgandAIVoiceTrace:I '*:S'
```
