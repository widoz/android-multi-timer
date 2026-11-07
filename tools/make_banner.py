"""Generates the README banner (light + dark) and the GitHub social preview as SVG.

Pixel lettering is drawn as rectangles, so the SVGs need no fonts for the title or times.
Run: python3 tools/make_banner.py   (writes docs/assets/*.svg)
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "docs" / "assets"

THEMES = {
    "dark": dict(bg="#121212", card="#1E1E1E", ink="#E3E3E3", muted="#A8A8A8", outline="#757575",
                 track="#3A3A3A", dot="#1C1C1C", done="#FF9A6B", done_bg="#4A2414",
                 colors=["#7FD3D3", "#F2B85B", "#C3B0FF", "#8FD694", "#FF9FB0"]),
    "light": dict(bg="#FFFFFF", card="#FFFFFF", ink="#1F1F1F", muted="#5F6368", outline="#8A8A8A",
                  track="#E0E0E0", dot="#F0F0F0", done="#B53C0B", done_bg="#FDE4D6",
                  colors=["#0F9AA0", "#D27206", "#8B5CF6", "#1F9E44", "#E8476A"]),
}

# 5x7 pixel font (only the glyphs the banner uses)
FONT = {
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "I": ["01110", "00100", "00100", "00100", "00100", "00100", "01110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "0": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    ":": ["00000", "01100", "01100", "00000", "01100", "01100", "00000"],
    " ": ["00000"] * 7,
}

STOPWATCH = [
    "......####......",
    ".......##.......",
    ".....######.....",
    "...##......##...",
    "..#..........#..",
    ".#.....#......#.",
    ".#.....#......#.",
    "#......#.......#",
    "#......####....#",
    "#..............#",
    "#..............#",
    ".#............#.",
    ".#............#.",
    "..#..........#..",
    "...##......##...",
    ".....######.....",
]

def pixels(rows, x, y, size):
    """Path data for the '1'/'#' cells of a bitmap."""
    d = []
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch in "1#":
                d.append(f"M{x + c * size} {y + r * size}h{size}v{size}h-{size}z")
    return "".join(d)

def text_path(text, x, y, size, advance=6):
    return "".join(pixels(FONT[ch], x + i * advance * size, y, size) for i, ch in enumerate(text))

def chip(x, y, label, t):
    w = round(len(label) * 9.7 + 28)
    return (f'<rect x="{x}" y="{y}" width="{w}" height="34" rx="8" fill="{t["card"]}" stroke="{t["outline"]}"/>'
            f'<text x="{x + w / 2}" y="{y + 22.5}" text-anchor="middle" class="mono">{label}</text>'), w

def card(x, y, label, sub, time, color, progress, t, done=False, idx=0):
    w, h = 340, 88
    border = t["done"] if done else t["outline"]
    fill = t["done_bg"] if done else t["card"]
    r, cx, cy = 24, x + 48, y + h / 2
    circ = 2 * 3.14159265 * r
    ring_color = t["done"] if done else color
    # The time: pixel digits, with the colon in its own group so it can blink.
    tx, ty, size = x + 96, y + 18, 5
    digits = "".join(pixels(FONT[ch], tx + i * 6 * size, ty, size) for i, ch in enumerate(time) if ch != ":")
    colon = "".join(pixels(FONT[":"], tx + i * 6 * size, ty, size) for i, ch in enumerate(time) if ch == ":")
    time_color = t["done"] if done else t["ink"]
    blink = "" if done else ' class="blink"'
    anim = "" if done else f' class="drain" style="--from:{circ * (1 - progress):.1f};--to:{circ * (1 - progress) + circ * 0.18:.1f};animation-delay:-{idx * 7}s"'
    return f'''
  <g>
    <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="18" fill="{fill}" stroke="{border}"/>
    <circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{t["track"]}" stroke-width="7"/>
    <circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{ring_color}" stroke-width="7" stroke-linecap="round"
            stroke-dasharray="{circ:.1f}" stroke-dashoffset="{circ * (1 - progress):.1f}" transform="rotate(-90 {cx} {cy})"{anim}/>
    <path d="{digits}" fill="{time_color}"/>
    <path d="{colon}" fill="{time_color}"{blink}/>
    <rect x="{tx}" y="{y + 63}" width="8" height="8" fill="{color}"/>
    <text x="{tx + 16}" y="{y + 71}" class="small">{label} · {sub}</text>
  </g>'''

def build(theme, height=360):
    t = THEMES[theme]
    off = (height - 360) // 2  # vertical offset for taller canvases (social preview)
    c = t["colors"]
    # Title: MULTI in the five timer colors, TIMER in ink, with a one-pixel retro shadow.
    tx, ty, size = 208, 62 + off, 9
    shadow = text_path("MULTI TIMER", tx + size, ty + size, size)
    letters = "".join(f'<path d="{pixels(FONT[ch], tx + i * 6 * size, ty, size)}" fill="{c[i]}"/>' for i, ch in enumerate("MULTI"))
    timer = f'<path d="{text_path("TIMER", tx + 6 * 6 * size, ty, size)}" fill="{t["ink"]}"/>'
    icon = pixels(STOPWATCH, 64, 52 + off, 7)
    hand = pixels(["#"], 64 + 7 * 7, 52 + off + 5 * 7, 7) + pixels(["#"], 64 + 7 * 7, 52 + off + 6 * 7, 7) \
        + pixels(["#"], 64 + 7 * 7, 52 + off + 7 * 7, 7) + pixels(["####"], 64 + 7 * 7, 52 + off + 8 * 7, 7)
    chips, x = [], 64
    for label in ["Android 11+", "Kotlin · Compose", "WCAG 2.2 AA", "Offline · no tracking"]:
        svg, w = chip(x, 262 + off, label, t); chips.append(svg); x += w + 10
    cards = (card(892, 24 + off, "Pasta", "Pixel bell", "06:48", c[1], 0.76, t, idx=0)
             + card(892, 136 + off, "Focaccia", "Power chime", "18:10", c[0], 0.73, t, idx=1)
             + card(892, 248 + off, "Tea steep", "Time's up", "00:00", c[3], 0.0, t, done=True))
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="{height}" viewBox="0 0 1280 {height}" role="img" aria-labelledby="title desc">
  <title id="title">Multi Timer</title>
  <desc id="desc">Multi Timer for Android: many countdowns at once, with retro 8-bit alarms.</desc>
  <defs>
    <pattern id="dots" width="16" height="16" patternUnits="userSpaceOnUse"><rect width="2" height="2" fill="{t["dot"]}"/></pattern>
    <style>
      text {{ font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; fill: {t["ink"]}; }}
      .mono {{ font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 15px; fill: {t["ink"]}; }}
      .small {{ font-size: 15px; fill: {t["muted"]}; }}
      .tag {{ font-size: 30px; font-weight: 600; }}
      .sub {{ font-size: 19px; fill: {t["muted"]}; }}
      .blink {{ animation: blink 1s steps(1) infinite; }}
      @keyframes blink {{ 50% {{ opacity: 0; }} }}
      .drain {{ animation: drain 20s linear infinite; }}
      @keyframes drain {{ from {{ stroke-dashoffset: var(--from); }} to {{ stroke-dashoffset: var(--to); }} }}
      @media (prefers-reduced-motion: reduce) {{ .blink, .drain {{ animation: none; }} }}
    </style>
  </defs>
  <rect width="1280" height="{height}" fill="{t["bg"]}"/>
  <rect width="1280" height="{height}" fill="url(#dots)"/>
  <path d="{icon}" fill="{t["ink"]}"/>
  <path d="{hand}" fill="{c[1]}"/>
  <path d="{shadow}" fill="{t["track"]}"/>
  {letters}
  {timer}
  <text x="64" y="{200 + off}" class="tag">Many countdowns at once. Retro 8-bit alarms.</text>
  <text x="64" y="{236 + off}" class="sub">A native Android timer app, built with Kotlin and Jetpack Compose.</text>
  {"".join(chips)}
  {cards}
</svg>
'''

if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for theme in THEMES:
        (OUT / f"banner-{theme}.svg").write_text(build(theme))
    (OUT / "social-preview.svg").write_text(build("dark", height=640))
    for f in sorted(OUT.glob("*.svg")):
        print(f.name, f.stat().st_size, "bytes")
