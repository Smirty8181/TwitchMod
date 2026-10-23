#!/usr/bin/env python3
"""Сборка витринных текстур TwitchCraft (правильные размеры, оптимизация)."""
from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "twitchcraft"
TOOLS = ROOT / "tools"
MODRINTH = ROOT / "modrinth"

SRC_ICON = ASSETS / "icon.png"
SRC_EGG = ASSETS / "textures" / "item" / "viewer_egg.png"
SRC_PHANTOM = TOOLS / "phantom_ref.png"
REF_EYES = TOOLS / "phantom_eyes_ref.png"


def save_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    img.save(path, format="PNG", optimize=True)
    print(f"  {path.relative_to(ROOT)}  ({path.stat().st_size:,} B, {img.size[0]}x{img.size[1]})")


def upscale_center(src: Image.Image, canvas: int, target_h: int) -> Image.Image:
    ratio = target_h / src.height
    nw = max(1, int(src.width * ratio))
    nh = target_h
    scaled = src.resize((nw, nh), Image.Resampling.NEAREST)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    out.paste(scaled, ((canvas - nw) // 2, (canvas - nh) // 2), scaled)
    return out


def make_smirty_face(size: int = 64) -> Image.Image:
    """8x8 симметричная голова Smirty — читается в маленьком размере."""
    yellow = (255, 215, 0, 255)
    grey = (138, 138, 152, 255)
    white = (255, 255, 255, 255)
    pupil = (24, 24, 34, 255)
    red = (255, 68, 68, 255)
    cols = [
        [0, 0, yellow, yellow, yellow, yellow, 0, 0],
        [0, yellow, yellow, yellow, yellow, yellow, yellow, 0],
        [yellow, grey, white, pupil, pupil, white, grey, yellow],
        [yellow, grey, white, pupil, pupil, white, grey, yellow],
        [yellow, grey, grey, red, red, grey, grey, yellow],
        [yellow, grey, yellow, yellow, yellow, yellow, grey, yellow],
        [0, yellow, yellow, yellow, yellow, yellow, yellow, 0],
        [0, 0, 0, 0, 0, 0, 0, 0],
    ]
    unit = size // 8
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for row in range(8):
        for col in range(8):
            c = cols[row][col]
            if c == 0:
                continue
            for dy in range(unit):
                for dx in range(unit):
                    px[col * unit + dx, row * unit + dy] = c
    return img


def make_smirty_logo() -> Image.Image:
    """64x64 бейдж: фиолетовый фон + золотая рамка + лицо по центру."""
    size = 64
    purple = (58, 18, 98, 255)
    gold = (255, 215, 0, 255)
    out = Image.new("RGBA", (size, size), purple)
    draw = ImageDraw.Draw(out)
    border = 4
    for i in range(border):
        draw.rectangle((i, i, size - 1 - i, size - 1 - i), outline=gold)
    face = make_smirty_face(48)
    out.paste(face, (8, 8), face)
    return out


def make_icon(size: int) -> Image.Image:
    logo = make_smirty_logo()
    return logo.resize((size, size), Image.Resampling.NEAREST)


def make_modrinth_icon() -> Image.Image:
    out = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(out)
    for y in range(256):
        t = y / 255
        r = int(18 + (92 - 18) * t)
        g = int(18 + (22 - 18) * t)
        b = int(24 + (150 - 24) * t)
        draw.line([(0, y), (255, y)], fill=(r, g, b, 255))
    draw.rounded_rectangle((24, 24, 231, 231), radius=28, fill=(20, 20, 28, 255), outline=(232, 197, 71, 255), width=4)
    face = make_smirty_logo().resize((140, 140), Image.Resampling.NEAREST)
    out.paste(face, (58, 50), face)
    return out


def make_keybind_icon() -> Image.Image:
    return make_smirty_logo().resize((16, 16), Image.Resampling.NEAREST)


def make_viewer_egg() -> Image.Image:
    """16x16 яйцо зрителя — чистый пиксель-арт."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    PURPLE = (123, 47, 190, 255)
    PURPLE_D = (75, 35, 130, 255)
    PURPLE_L = (170, 110, 220, 255)
    OUTLINE = (45, 20, 80, 255)
    GOLD = (255, 215, 0, 255)
    GOLD_D = (200, 160, 0, 255)
    WHITE = (255, 255, 255, 255)
    BLACK = (30, 30, 40, 255)

    egg_pixels = {
        (4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 1),
        (3, 2), (4, 2), (5, 2), (6, 2), (7, 2), (8, 2), (9, 2), (10, 2), (11, 2), (12, 2),
        (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (11, 3), (12, 3), (13, 3),
        (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4), (12, 4), (13, 4),
        (1, 5), (2, 5), (3, 5), (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5), (11, 5), (12, 5), (13, 5), (14, 5),
        (1, 6), (2, 6), (3, 6), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6), (12, 6), (13, 6), (14, 6),
        (1, 7), (2, 7), (3, 7), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (12, 7), (13, 7), (14, 7),
        (2, 8), (3, 8), (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (12, 8), (13, 8),
        (2, 9), (3, 9), (4, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (12, 9), (13, 9),
        (3, 10), (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (11, 10), (12, 10),
        (4, 11), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11), (11, 11),
        (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12),
        (6, 13), (7, 13), (8, 13), (9, 13),
    }
    for x, y in egg_pixels:
        shade = PURPLE_L if x <= 6 and y <= 6 else (PURPLE if y <= 9 else PURPLE_D)
        px[x, y] = shade

    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0:
                continue
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] == 0:
                    px[nx, ny] = OUTLINE

    for x, y in ((3, 3), (4, 2), (5, 3), (4, 4), (3, 4)):
        px[x, y] = GOLD
    px[4, 3] = GOLD_D

    ghost = [
        (10, 7, WHITE), (11, 7, WHITE), (12, 7, WHITE),
        (10, 8, WHITE), (11, 8, BLACK), (12, 8, WHITE),
        (10, 9, WHITE), (11, 9, WHITE), (12, 9, WHITE),
        (10, 10, WHITE), (11, 10, WHITE), (12, 10, WHITE),
        (10, 11, WHITE), (12, 11, WHITE),
    ]
    for x, y, c in ghost:
        px[x, y] = c

    for x, y in ((7, 5), (9, 6), (6, 10), (13, 4)):
        px[x, y] = WHITE

    return img


def _recolor_phantom_pixel(r: int, g: int, b: int, a: int) -> tuple[int, int, int, int]:
    if a == 0:
        return (0, 0, 0, 0)
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    # Глаза остаются на отдельном слое — на теле убираем яркие бирюзовые пиксели
    if g > 170 and b > 170 and r < 130:
        return (0, 0, 0, 0)
    if lum > 205:
        return (210, 150, 245, a)
    if lum > 140:
        return (140, 55, 200, a)
    if lum > 75:
        return (85, 30, 135, a)
    return (40, 14, 72, a)


def optimize_chat_phantom() -> Image.Image:
    src = Image.open(SRC_PHANTOM).convert("RGBA")
    if src.size != (64, 64):
        src = src.resize((64, 64), Image.Resampling.NEAREST)
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px_in = src.load()
    px_out = out.load()
    for y in range(64):
        for x in range(64):
            px_out[x, y] = _recolor_phantom_pixel(*px_in[x, y])
    return out


def make_chat_phantom_procedural() -> Image.Image:
    """64x64 фиолетовый фантом (UV как у vanilla phantom) — без референса."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    dark = (40, 14, 72, 255)
    mid = (85, 30, 135, 255)
    light = (140, 55, 200, 255)
    highlight = (210, 150, 245, 255)

    draw.polygon([(0, 6), (30, 2), (34, 18), (18, 26), (2, 22)], fill=dark)
    draw.polygon([(30, 2), (64, 6), (62, 22), (46, 26), (34, 18)], fill=dark)
    draw.polygon([(4, 10), (26, 6), (30, 16), (14, 20)], fill=mid)
    draw.polygon([(34, 6), (60, 10), (56, 20), (38, 16)], fill=mid)
    draw.ellipse((18, 16, 46, 40), fill=mid)
    draw.ellipse((20, 18, 44, 36), fill=light)
    draw.ellipse((22, 10, 42, 28), fill=light)
    draw.ellipse((24, 12, 40, 24), fill=highlight)
    draw.polygon([(28, 36), (36, 36), (32, 54), (26, 50)], fill=dark)
    draw.polygon([(30, 38), (34, 38), (32, 48)], fill=mid)
    return img


def make_chat_phantom_body() -> Image.Image:
    if SRC_PHANTOM.exists():
        return optimize_chat_phantom()
    print("  (no phantom_ref.png — procedural chat_phantom)")
    return make_chat_phantom_procedural()


def make_chat_phantom_eyes() -> Image.Image:
    if REF_EYES.exists():
        ref = Image.open(REF_EYES).convert("RGBA")
        out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        ref_px = ref.load()
        out_px = out.load()
        for y in range(64):
            for x in range(64):
                r, g, b, a = ref_px[x, y]
                if a > 0 and (r + g + b) > 40:
                    out_px[x, y] = (0, 255, 255, min(255, a + 80))
        return out
    print("  (no phantom_eyes_ref.png — procedural eyes)")
    return make_chat_phantom_eyes_procedural()


def make_chat_phantom_eyes_procedural() -> Image.Image:
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for cx, cy in ((27, 17), (35, 17)):
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 5:
                    px[cx + dx, cy + dy] = (0, 255, 255, 255)
    return img


def make_wheel_frame(size: int = 128) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx = cy = size // 2
    outer = size // 2 - 2
    inner = outer - 16

    draw.ellipse((cx - outer, cy - outer, cx + outer, cy + outer), fill=(18, 12, 8, 255))
    draw.ellipse((cx - outer + 2, cy - outer + 2, cx + outer - 2, cy + outer - 2), fill=(255, 230, 140, 255))
    draw.ellipse((cx - outer + 5, cy - outer + 5, cx + outer - 5, cy + outer - 5), fill=(232, 197, 71, 255))
    draw.ellipse((cx - outer + 8, cy - outer + 8, cx + outer - 8, cy + outer - 8), fill=(160, 104, 18, 255))
    draw.ellipse((cx - outer + 11, cy - outer + 11, cx + outer - 11, cy + outer - 11), fill=(255, 220, 110, 255))
    draw.ellipse((cx - inner, cy - inner, cx + inner, cy + inner), fill=(10, 8, 16, 255))
    draw.ellipse((cx - inner + 3, cy - inner + 3, cx + inner - 3, cy + inner - 3), fill=(22, 16, 32, 255))

    for i in range(8):
        ang = math.radians(i * 45 - 90)
        lx = cx + int(math.cos(ang) * (outer - 8))
        ly = cy + int(math.sin(ang) * (outer - 8))
        gem = (168, 82, 255, 255) if i % 2 == 0 else (255, 80, 90, 255)
        draw.ellipse((lx - 5, ly - 5, lx + 5, ly + 5), fill=(28, 18, 10, 255))
        draw.ellipse((lx - 3, ly - 3, lx + 3, ly + 3), fill=gem)
        draw.point((lx - 1, ly - 1), fill=(255, 255, 255, 220))

    draw.ellipse((cx - 8, cy - 8, cx + 8, cy + 8), fill=(28, 20, 38, 255), outline=(232, 197, 71, 255), width=2)
    draw.ellipse((cx - 3, cy - 3, cx + 3, cy + 3), fill=(255, 215, 0, 255))
    return img


def make_wheel_pointer() -> Image.Image:
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.polygon([(16, 1), (28, 16), (21, 16), (21, 30), (11, 30), (11, 16), (4, 16)], fill=(120, 78, 12, 255))
    draw.polygon([(16, 3), (26, 15), (20, 15), (20, 28), (12, 28), (12, 15), (6, 15)], fill=(232, 197, 71, 255))
    draw.polygon([(16, 5), (22, 14), (10, 14)], fill=(255, 245, 200, 255))
    draw.rectangle((14, 16, 17, 22), fill=(168, 82, 255, 255))
    draw.point((15, 17), fill=(255, 255, 255, 255))
    return img


def make_donation_badge() -> Image.Image:
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((2, 2, 29, 29), radius=6, fill=(92, 22, 150, 255), outline=(232, 197, 71, 255), width=2)
    draw.ellipse((10, 10, 21, 21), fill=(255, 215, 0, 255), outline=(184, 134, 11, 255))
    draw.rectangle((14, 13, 17, 20), fill=(92, 22, 150, 255))
    draw.rectangle((13, 15, 14, 17), fill=(92, 22, 150, 255))
    return img


def make_wheel_icons() -> None:
    import sys
    sys.path.insert(0, str(TOOLS))
    from wheel_pixel_art import WHEEL_ICON_BUILDERS, write_java

    out_dir = ASSETS / "textures" / "gui" / "wheel"
    for name, maker in WHEEL_ICON_BUILDERS.items():
        save_png(maker(), out_dir / f"{name}.png")
    write_java(ROOT / "src" / "client" / "java" / "net" / "twitchcraft" / "client" / "WheelIconSprites.java")


def main() -> None:
    print("Building TwitchCraft textures...")
    save_png(make_smirty_logo(), ASSETS / "textures" / "gui" / "smirty_logo.png")
    save_png(make_icon(128), ASSETS / "icon.png")
    save_png(make_modrinth_icon(), MODRINTH / "icon.png")
    save_png(make_keybind_icon(), ASSETS / "textures" / "gui" / "sprites" / "keybinds" / "category" / "main.png")
    save_png(make_viewer_egg(), ASSETS / "textures" / "item" / "viewer_egg.png")
    save_png(make_chat_phantom_body(), ASSETS / "textures" / "entity" / "chat_phantom.png")
    save_png(make_chat_phantom_eyes(), ASSETS / "textures" / "entity" / "chat_phantom_eyes.png")
    save_png(make_wheel_frame(), ASSETS / "textures" / "gui" / "wheel_frame.png")
    save_png(make_wheel_pointer(), ASSETS / "textures" / "gui" / "wheel_pointer.png")
    save_png(make_donation_badge(), ASSETS / "textures" / "gui" / "donation_badge.png")
    make_wheel_icons()

    ref = ASSETS / "textures" / "gui" / "wheel_reference.png"
    if ref.exists():
        TOOLS.mkdir(exist_ok=True)
        ref.rename(TOOLS / "wheel_reference.png")
        print("  moved wheel_reference.png -> tools/ (dev only)")

    print("Done.")


if __name__ == "__main__":
    main()
