"""Render the matching 512 px Google Play listing icon (no external assets)."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


SIZE = 512
SCALE = 3
N = SIZE * SCALE
OUT = Path(__file__).with_name("play-store-icon.png")


def box(values):
    return tuple(round(value * SCALE) for value in values)


def rounded(draw, bounds, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(
        box(bounds), radius=radius * SCALE, fill=fill, outline=outline, width=width * SCALE
    )


background = Image.new("RGB", (N, N))
pixels = background.load()
for y in range(N):
    for x in range(N):
        t = (x / N * 0.3 + y / N * 0.7)
        glow = max(0, 1 - ((x / N - 0.68) ** 2 + (y / N - 0.16) ** 2) * 2.7)
        pixels[x, y] = (
            round(23 + 11 * t + 8 * glow),
            round(29 + 13 * t + 11 * glow),
            round(37 + 16 * t + 14 * glow),
        )

icon = background.convert("RGBA")
shadow = Image.new("RGBA", (N, N))
shadow_draw = ImageDraw.Draw(shadow)
rounded(shadow_draw, (103, 61, 409, 453), 68, (0, 0, 0, 150))
shadow = shadow.filter(ImageFilter.GaussianBlur(22 * SCALE))
icon = Image.alpha_composite(icon, shadow)

face = Image.new("RGBA", (N, N))
draw = ImageDraw.Draw(face)
rounded(draw, (105, 58, 407, 450), 67, (50, 61, 73, 248), (147, 162, 177, 164), 2)
rounded(draw, (111, 64, 401, 444), 62, (43, 53, 65, 120), (241, 246, 250, 52), 1)
rounded(draw, (132, 94, 380, 210), 29, (28, 38, 48, 238), (177, 195, 208, 101), 2)

# A short function curve suggests the calculator's scientific tools.
draw.line([box((158, 171)), box((196, 171)), box((217, 149)), box((248, 149)),
           box((271, 128)), box((310, 128)), box((333, 113)), box((351, 113))],
          fill=(229, 238, 244, 230), width=5 * SCALE, joint="curve")
for x, y in ((217, 149), (271, 128), (333, 113)):
    draw.ellipse(box((x - 4, y - 4, x + 4, y + 4)), fill=(243, 248, 250, 241))

for row in range(3):
    for col in range(3):
        x = 132 + col * 84
        y = 237 + row * 68
        is_equal = row == 2 and col == 2
        fill = (232, 239, 243, 245) if is_equal else (95, 109, 123, 173)
        border = (250, 252, 253, 122) if is_equal else (197, 209, 218, 70)
        rounded(draw, (x, y, x + 64, y + 51), 17, fill, border, 1)
        if is_equal:
            draw.line(box((x + 20, y + 21, x + 44, y + 21)), fill=(44, 57, 68), width=3 * SCALE)
            draw.line(box((x + 20, y + 31, x + 44, y + 31)), fill=(44, 57, 68), width=3 * SCALE)
        else:
            draw.ellipse(box((x + 27, y + 20, x + 37, y + 30)), fill=(212, 223, 232, 180))

icon = Image.alpha_composite(icon, face).convert("RGB")
icon.resize((SIZE, SIZE), Image.Resampling.LANCZOS).save(OUT, optimize=True)
print(f"{OUT}: {OUT.stat().st_size} bytes")
