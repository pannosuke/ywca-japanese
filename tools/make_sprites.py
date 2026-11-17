#!/usr/bin/env python3
"""Original pixel sprites. No third-party art."""
import struct
import zlib
from pathlib import Path

ROOT = Path("/workspace")


def chunk(tag: bytes, data: bytes) -> bytes:
    crc = zlib.crc32(tag + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)


def write_png(path: Path, width: int, height: int, pixels) -> None:
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(pixels(x, y))
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)


def hero(x, y):
    if 26 <= x <= 38 and 4 <= y <= 10:
        return (70, 48, 32, 255)
    if 24 <= x <= 40 and 8 <= y <= 24:
        return (242, 214, 176, 255)
    if x in (28, 36) and 14 <= y <= 16:
        return (40, 28, 18, 255)
    if 14 <= x <= 50 and 24 <= y <= 70:
        return (47, 122, 64, 255)
    if 28 <= x <= 36 and 36 <= y <= 48:
        return (232, 186, 64, 255)
    if 18 <= x <= 28 and 70 <= y <= 92:
        return (92, 64, 40, 255)
    if 36 <= x <= 46 and 70 <= y <= 92:
        return (92, 64, 40, 255)
    return (0, 0, 0, 0)


def blob(color):
    def pixel(x, y):
        dx = x - 32
        dy = y - 36
        if dx * dx + dy * dy <= 26 * 26:
            if 22 <= x <= 26 and 30 <= y <= 34 or 38 <= x <= 42 and 30 <= y <= 34:
                return (30, 24, 20, 255)
            return color
        return (0, 0, 0, 0)
    return pixel


def tree(x, y):
    dx = x - 32
    dy = y - 28
    if dx * dx / 28 + dy * dy / 18 <= 28:
        shade = 40 + (x % 7) * 4
        return (46, 120 + shade // 5, 52, 255)
    if 28 <= x <= 36 and 48 <= y <= 92:
        return (110, 74, 42, 255)
    return (0, 0, 0, 0)


def house(x, y):
    if 8 <= x <= 56 and 36 <= y <= 88:
        return (196, 164, 120, 255)
    if y < 40 and abs(x - 32) <= (40 - y):
        return (92, 62, 46, 255)
    if 26 <= x <= 38 and 58 <= y <= 88:
        return (86, 54, 36, 255)
    if 12 <= x <= 22 and 48 <= y <= 60:
        return (255, 214, 120, 255)
    return (0, 0, 0, 0)


def icon(x, y):
    dx = x - 64
    dy = y - 64
    if dx * dx + dy * dy > 58 * 58:
        return (0, 0, 0, 0)
    if dx * dx + dy * dy < 16 * 16:
        return (232, 186, 64, 255)
    return (36, 110, 62, 255)


def main() -> None:
    sprites = ROOT / "assets" / "sprites"
    write_png(sprites / "hero.png", 64, 96, hero)
    write_png(sprites / "enemy_godan.png", 64, 64, blob((86, 92, 150, 255)))
    write_png(sprites / "enemy_ichidan.png", 64, 64, blob((176, 92, 64, 255)))
    write_png(sprites / "enemy_irregular.png", 64, 64, blob((120, 64, 130, 255)))
    write_png(sprites / "tree.png", 64, 96, tree)
    write_png(sprites / "house.png", 64, 96, house)
    write_png(ROOT / "icon.png", 128, 128, icon)


if __name__ == "__main__":
    main()
