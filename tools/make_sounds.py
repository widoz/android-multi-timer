"""Original 8-bit (chiptune) alarm sounds for Multi Timer, synthesized from scratch.

Regenerate: python3 tools/make_sounds.py (writes NAME.wav in the current folder), then for each:
    ffmpeg -i NAME.wav -c:a libvorbis -q:a 5 app/src/main/res/raw/NAME.ogg

Console-style building blocks: pulse waves (12.5/25/50% duty), a triangle wave, a noise
channel, volume envelopes stepped at 60 Hz, arpeggios instead of chords, and output reduced
to 8-bit depth. Melodies are original. Each file is one phrase plus a rest, for looping.
"""
import wave, numpy as np

SR = 44100
FRAME = SR // 60  # sound chips update volume once per video frame
rng = np.random.default_rng(7)

def note(name):
    names = {"C": -9, "C#": -8, "D": -7, "D#": -6, "E": -5, "F": -4, "F#": -3,
             "G": -2, "G#": -1, "A": 0, "A#": 1, "B": 2}
    pitch, octave = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[pitch] + (octave - 4) * 12) / 12)

def stepped(n, start=15, decay_frames=2, floor=0):
    """Volume 0..15 dropping one step every `decay_frames` frames, like a chip envelope."""
    frames = np.arange(n) // FRAME
    vol = np.maximum(floor, start - frames // decay_frames)
    return vol / 15.0

def pulse(freq, dur, duty=0.5, **env):
    n = int(dur * SR); t = np.arange(n) / SR
    phase = (t * freq) % 1.0
    return np.where(phase < duty, 1.0, -1.0) * stepped(n, **env)

def triangle(freq, dur, **env):
    n = int(dur * SR); t = np.arange(n) / SR
    phase = (t * freq) % 1.0
    tri = 4 * np.abs(phase - 0.5) - 1
    tri = np.round((tri + 1) * 7.5) / 7.5 - 1  # 16-step triangle, like the NES
    return tri * stepped(n, **env)

def noise(dur, **env):
    n = int(dur * SR)
    # sample-and-hold noise at ~11 kHz for a crunchy chip "tick"
    hold = 4
    raw = np.repeat(rng.choice([-1.0, 1.0], size=n // hold + 1), hold)[:n]
    return raw * stepped(n, **env)

def arp(notes, dur, step=1/60, duty=0.25, **env):
    """Cycle quickly through notes each frame: the chiptune way to play a chord."""
    n = int(dur * SR); out = np.zeros(n); t = np.arange(n) / SR
    idx = (np.arange(n) // int(step * SR)) % len(notes)
    freqs = np.array([note(x) for x in notes])[idx]
    phase = np.cumsum(freqs) / SR % 1.0
    out = np.where(phase < duty, 1.0, -1.0)
    return out * stepped(n, **env)

def place(total, events):
    buf = np.zeros(int(total * SR))
    for start, sig, gain in events:
        i = int(start * SR); j = min(len(buf), i + len(sig))
        buf[i:j] += gain * sig[: j - i]
    return buf

def dc_block(x, r=0.995):
    """High-pass around 35 Hz: removes the DC offset of uneven pulse waves (as console
    audio outputs do), which would otherwise cause thumps and waste headroom."""
    y = np.zeros_like(x); prev_x = prev_y = 0.0
    for i, v in enumerate(x):
        prev_y = v - prev_x + r * prev_y; prev_x = v; y[i] = prev_y
    return y

def finish(x, target_rms_db=-18.0, peak_db=-1.0):
    x = dc_block(x)
    f = int(0.003 * SR); x = x.copy(); x[:f] *= np.linspace(0, 1, f); x[-f:] *= np.linspace(1, 0, f)
    rms = np.sqrt(np.mean(x[np.abs(x) > 1e-4] ** 2))
    x *= 10 ** (target_rms_db / 20) / rms
    peak = np.max(np.abs(x)); cap = 10 ** (peak_db / 20)
    if peak > cap: x *= cap / peak
    return np.round(x * 127) / 127  # 8-bit depth

# --- the five sounds -----------------------------------------------------------------

def power_chime():
    """Quick rising arpeggio C5-E5-G5-C6, then an echo an octave up, quieter."""
    seq = ["C5", "E5", "G5", "C6"]
    ev = [(i * 0.07, pulse(note(n), 0.16, 0.25, decay_frames=1), 1.0) for i, n in enumerate(seq)]
    ev += [(0.42 + i * 0.07, pulse(note(n) * 2, 0.14, 0.125, start=9, decay_frames=1), 0.8) for i, n in enumerate(seq)]
    ev += [(0.0, triangle(note("C3"), 0.5, decay_frames=3), 0.9)]
    return place(1.6, ev)

def pixel_bell():
    """Two square-wave 'bell' strikes: a held note with a fast arpeggio shimmer."""
    ev = []
    for start in (0.0, 0.5):
        ev.append((start, arp(["A5", "E6", "A6"], 0.45, step=1/60, duty=0.5, decay_frames=2), 1.0))
        ev.append((start, noise(0.05, start=10, decay_frames=1), 0.35))
    ev.append((0.0, triangle(note("A3"), 0.9, decay_frames=4), 0.8))
    return place(1.7, ev)

def beeper():
    """Classic chip alarm: two-tone beeps E6/B5, four times."""
    ev = []
    for i in range(4):
        ev.append((i * 0.16, pulse(note("E6" if i % 2 == 0 else "B5"), 0.11, 0.5, start=13, decay_frames=6), 1.0))
    return place(1.2, ev)

def gentle_rise():
    """Soft triangle and thin pulse notes climbing slowly: a calmer 8-bit wake-up."""
    seq = ["C4", "E4", "G4", "B4", "D5"]
    ev = []
    for i, n in enumerate(seq):
        start = i * 0.32
        vol = 6 + i * 2  # gets a little louder on each step
        ev.append((start, triangle(note(n), 0.6, start=15, decay_frames=6), 0.9))
        ev.append((start, pulse(note(n) * 2, 0.3, 0.125, start=min(vol, 15), decay_frames=4), 0.35))
    return place(2.8, ev)

def jingle():
    """A short original tune with a bouncing triangle bass and noise hi-hats."""
    melody = [("G5", 0.00), ("C6", 0.12), ("B5", 0.24), ("G5", 0.36), ("E6", 0.54), ("D6", 0.72)]
    bass = [("C3", 0.00), ("G3", 0.24), ("C3", 0.48), ("G3", 0.72)]
    ev = [(t, pulse(note(n), 0.16 if n != "D6" else 0.3, 0.25, decay_frames=2), 1.0) for n, t in melody]
    ev += [(t, triangle(note(n), 0.2, decay_frames=3), 0.9) for n, t in bass]
    ev += [(i * 0.12, noise(0.03, start=8, decay_frames=1), 0.3) for i in range(8)]
    return place(1.5, ev)

SOUNDS = {
    "power_chime": power_chime, "pixel_bell": pixel_bell, "beeper": beeper,
    "gentle_rise": gentle_rise, "jingle": jingle,
}

if __name__ == "__main__":
    for name, fn in SOUNDS.items():
        x = finish(fn())
        pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
        with wave.open(f"{name}.wav", "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        print(f"{name:12s} {len(x)/SR:4.2f}s  peak {20*np.log10(np.max(np.abs(x))):5.1f} dBFS")
