"""Makes the sound of the gas alert: app/src/main/res/raw/gas_alarm.ogg.

Three short, high beeps and a pause, four times (about 6 seconds), like a smoke detector, so it cannot be taken for the alarm clock. Android repeats
the sound of the notification until it is opened, so the file only has to be one clear warning. The beeps carry two overtones, which a phone speaker
plays louder than the pure tone.

Needs numpy and ffmpeg (with libvorbis). From the android/ folder:

    python3 tools/make_alarm_sound.py
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

SAMPLE_RATE = 44100
BEEP_HZ = 2900
BEEP_SECONDS = 0.16
GAP_SECONDS = 0.11
PAUSE_SECONDS = 0.75
CYCLES = 4
FADE_SECONDS = 0.008
OUTPUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw/gas_alarm.ogg"


def beep() -> np.ndarray:
    t = np.arange(int(SAMPLE_RATE * BEEP_SECONDS)) / SAMPLE_RATE
    tone = sum(weight * np.sin(2 * np.pi * BEEP_HZ * multiple * t) for multiple, weight in ((1, 1.0), (2, 0.45), (3, 0.2)))
    # A short fade at each end, so the beep does not click.
    envelope = np.minimum(1, np.minimum(t / FADE_SECONDS, (BEEP_SECONDS - t) / FADE_SECONDS))
    return tone * envelope


def silence(seconds: float) -> np.ndarray:
    return np.zeros(int(SAMPLE_RATE * seconds))


def main() -> int:
    cycle = np.concatenate([beep(), silence(GAP_SECONDS), beep(), silence(GAP_SECONDS), beep(), silence(PAUSE_SECONDS)])
    samples = np.tile(cycle, CYCLES)
    samples = samples / np.max(np.abs(samples)) * 0.95
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as folder:
        wav = Path(folder) / "gas_alarm.wav"
        with wave.open(str(wav), "wb") as out:
            out.setnchannels(1)
            out.setsampwidth(2)
            out.setframerate(SAMPLE_RATE)
            out.writeframes((samples * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav), "-map_metadata", "-1", "-c:a", "libvorbis", "-q:a", "4", str(OUTPUT)], check=True)
    print(f"{OUTPUT} ({OUTPUT.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
