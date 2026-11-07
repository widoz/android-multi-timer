"""Composes docs/assets/app-light.png and app-dark.png (rendered by ReadmeScreenshotTest)
into docs/assets/app-preview.png: two rounded phone frames side by side, transparent background.
Run after: ./gradlew testDebugUnitTest -Pscreenshots
"""
from pathlib import Path
from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parent.parent / "docs" / "assets"
SCREEN_W = 600            # each screenshot is scaled to this width
BEZEL, RADIUS, GAP = 14, 54, 56

def phone(path):
    shot = Image.open(path).convert("RGBA")
    shot = shot.resize((SCREEN_W, round(shot.height * SCREEN_W / shot.width)), Image.LANCZOS)
    w, h = shot.width + 2 * BEZEL, shot.height + 2 * BEZEL
    frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, w - 1, h - 1), RADIUS + BEZEL, fill=(32, 36, 38, 255))
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, shot.width - 1, shot.height - 1), RADIUS, fill=255)
    frame.paste(shot, (BEZEL, BEZEL), mask)
    return frame

left, right = phone(ASSETS / "app-light.png"), phone(ASSETS / "app-dark.png")
out = Image.new("RGBA", (left.width + GAP + right.width, max(left.height, right.height)), (0, 0, 0, 0))
out.paste(left, (0, 0), left)
out.paste(right, (left.width + GAP, 0), right)
out.save(ASSETS / "app-preview.png", optimize=True)
print("app-preview.png", out.size, (ASSETS / "app-preview.png").stat().st_size, "bytes")
