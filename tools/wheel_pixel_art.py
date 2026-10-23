"""Иконки рулетки — 16×16 как предметы Minecraft, апскейл ×3 в 48×48.

Без тёмной медали. Прозрачный фон, жирный чёрный контур, яркая заливка.
Читаются на тёмном барабане с 5 метров.
"""
from __future__ import annotations

import inspect
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

SPRITE_ROWS: dict[str, list[str]] = {}

CARD = 48
GRID = 16
SCALE = CARD // GRID

# Яркая палитра, без грязно-коричневого «на тёмном не видно»
P = {
    "k": (10, 8, 12, 255),
    "w": (255, 255, 255, 255),
    "y": (255, 220, 40, 255),
    "Y": (255, 248, 180, 255),
    "o": (255, 140, 20, 255),
    "O": (255, 90, 16, 255),
    "r": (230, 40, 40, 255),
    "R": (160, 16, 20, 255),
    "p": (255, 90, 160, 255),
    "g": (50, 210, 70, 255),
    "G": (20, 130, 40, 255),
    "b": (50, 170, 255, 255),
    "B": (20, 90, 200, 255),
    "c": (40, 230, 230, 255),
    "C": (0, 150, 170, 255),
    "v": (180, 80, 255, 255),
    "V": (110, 30, 190, 255),
    "n": (180, 110, 50, 255),
    "N": (110, 60, 24, 255),
    "s": (210, 214, 222, 255),
    "S": (120, 126, 138, 255),
    "d": (50, 52, 60, 255),
    "l": (255, 228, 190, 255),
    "a": (255, 70, 20, 255),
    "e": (80, 255, 160, 255),
    "m": (255, 60, 200, 255),
    "f": (255, 200, 60, 255),
    "u": (90, 60, 40, 255),  # ant body — тёплый, не серый
    "U": (140, 90, 50, 255),
    "x": (255, 40, 40, 255),
}

T = (0, 0, 0, 0)


def _outline(px: dict[tuple[int, int], tuple[int, int, int, int]]) -> None:
    extra: list[tuple[int, int]] = []
    for (x, y), c in px.items():
        if c[3] == 0:
            continue
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = x + dx, y + dy
            if not (0 <= nx < GRID and 0 <= ny < GRID):
                continue
            if (nx, ny) not in px:
                extra.append((nx, ny))
    for x, y in extra:
        px[(x, y)] = P["k"]


def _pad_row(row: str) -> str:
    if len(row) > GRID:
        raise ValueError(f"row too long ({len(row)}): {row}")
    if len(row) == GRID:
        return row
    pad = GRID - len(row)
    left = pad // 2
    return "." * left + row + "." * (pad - left)


def _center_rows(rows: list[str]) -> list[str]:
    minx, miny, maxx, maxy = GRID, GRID, -1, -1
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                minx = min(minx, x)
                maxx = max(maxx, x)
                miny = min(miny, y)
                maxy = max(maxy, y)
    if maxx < 0:
        return rows
    dx = int(round((GRID - 1) / 2 - (minx + maxx) / 2))
    dy = int(round((GRID - 1) / 2 - (miny + maxy) / 2))
    if dx == 0 and dy == 0:
        return rows
    out = ["." * GRID for _ in range(GRID)]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            nx, ny = x + dx, y + dy
            if 0 <= nx < GRID and 0 <= ny < GRID:
                s = out[ny]
                out[ny] = s[:nx] + ch + s[nx + 1:]
    return out


def _from_rows(rows: list[str]) -> Image.Image:
    rows = [_pad_row(r) for r in rows]
    if len(rows) < GRID:
        extra = GRID - len(rows)
        top = extra // 2
        rows = ["." * GRID] * top + rows + ["." * GRID] * (extra - top)
    if len(rows) != GRID:
        raise ValueError(f"sprite must be {GRID} rows, got {len(rows)}")
    rows = _center_rows(rows)
    caller = inspect.stack()[1].function
    if caller.startswith("icon_"):
        SPRITE_ROWS[caller[5:]] = list(rows)
    px: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch not in P:
                raise ValueError(f"unknown color {ch!r} at {x},{y}")
            px[(x, y)] = P[ch]
    _outline(px)
    img = Image.new("RGBA", (CARD, CARD), T)
    load = img.load()
    for (x, y), c in px.items():
        for dy in range(SCALE):
            for dx in range(SCALE):
                load[x * SCALE + dx, y * SCALE + dy] = c
    return img


def icon_meteor() -> Image.Image:
    return _from_rows([
        ".........YY...",
        "........YYo...",
        ".......YYoo...",
        "......YYooO...",
        "....YYYYooO...",
        "...YYYYYoo....",
        "..YYYYYYo.....",
        ".YYYooooa.....",
        ".YYooaaaa.....",
        "..YaaaaRR.....",
        "...aaaRRR.....",
        "....aaRR......",
        ".....aR.......",
        "..............",
        "..............",
        "..............",
    ])


def icon_wither() -> Image.Image:
    return _from_rows([
        "..............",
        ".sss...sss....",
        "swwws.swwws...",
        "swdws.swdws.ss",
        "swwws.swwwssww",
        ".sSs...sSsswdw",
        "..s.sss.s.swww",
        "....sSs....sSs",
        ".....s........",
        "...ssSss......",
        "...sSSSs......",
        "....sss.......",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_army() -> Image.Image:
    return _from_rows([
        ".s...s...s....",
        "sss.sss.sss...",
        ".s...s...s....",
        ".vv..vv..vv...",
        "vvvvvvvvvvvv..",
        "vVVvvVVvvVVv..",
        "vvvvvvvvvvvv..",
        ".vv..vv..vv...",
        ".vv..vv..vv...",
        ".vv..vv..vv...",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_tntrain() -> Image.Image:
    return _from_rows([
        "......oo......",
        "......oY......",
        ".....nnn......",
        "...rrrrrrr....",
        "..rrrrrrrrr...",
        "..rrwwwwwrr...",
        "..rrwrrrwrr...",
        "..rrwrrrwrr...",
        "..rrwwwwwrr...",
        "..rrrrrrrrr...",
        "..rrwwwwwrr...",
        "..rrrrrrrrr...",
        "...rrrrrrr....",
        "..............",
        "..............",
        "..............",
    ])


def icon_boss() -> Image.Image:
    return _from_rows([
        ".R........R...",
        "RrR......RrR..",
        ".RrrrrrrrrR...",
        ".rrrrrrrrrr...",
        "rrwwrrrrwwrr..",
        "rrddrrrrddrr..",
        "rrrrrrrrrrrr..",
        "rrrryyyyrrrr..",
        ".rrr....rrr...",
        ".rr.rrrr.rr...",
        "..r......r....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_dragon() -> Image.Image:
    return _from_rows([
        "..............",
        "........vv....",
        "V......vvvv...",
        "VV....vvVVvv..",
        ".VVvvvvVVVVv..",
        "..vvvvvVpVvv..",
        "...vvvVVVVvvv.",
        "....vvVwwVvvv.",
        "....vvVdVvv...",
        "....vvvvvv....",
        "...vv..vv.....",
        "..vv..........",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_prison() -> Image.Image:
    return _from_rows([
        "..............",
        ".ssssssssss...",
        ".s.s.s.s.ss...",
        ".s.s.s.s.ss...",
        ".s.s.s.s.ss...",
        ".s.s.s.s.ss...",
        ".s.syyy.sss...",
        ".s.syNy.sss...",
        ".s.syyy.sss...",
        ".s.s.s.s.ss...",
        ".s.s.s.s.ss...",
        ".s.s.s.s.ss...",
        ".ssssssssss...",
        "..............",
        "..............",
        "..............",
    ])


def icon_creeperstorm() -> Image.Image:
    return _from_rows([
        "..............",
        "..gggggggg....",
        ".gggggggggg...",
        ".ggddggddgg...",
        ".ggddggddgg...",
        ".gggggggggg...",
        ".gggddddggg...",
        ".ggddggddgg...",
        ".ggddggddgg...",
        ".ggddggddgg...",
        ".gggggggggg...",
        "..gggggggg....",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_event() -> Image.Image:
    return _from_rows([
        "..............",
        ".......y......",
        "......yYy.....",
        "......yYy.....",
        "..yyyyYYYyyyy.",
        "...yyYYYYYyy..",
        "....yYYYYYy...",
        ".....yYYYy....",
        "....yyYrYyy...",
        "...yy.rRr.yy..",
        "..yy...R...yy.",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_spin() -> Image.Image:
    return _from_rows([
        "..............",
        "....bbbb......",
        "...bbwwbb.....",
        "..bb....b.....",
        ".bb......bb...",
        ".b...bb...b...",
        ".b..bwwb..bb..",
        ".bb.bwwb...b..",
        "..b...bb...b..",
        "..bb......bb..",
        "....b....bb...",
        "....bbwwbb....",
        ".....bbbb.....",
        "..............",
        "..............",
        "..............",
    ])


def icon_curse() -> Image.Image:
    return _from_rows([
        "..............",
        "....eeeee.....",
        "...eeeeeee....",
        "..eeeddedee...",
        "..eeeddedee...",
        "..eeeeeeeee...",
        "...eeedeee....",
        "....eeeee.....",
        ".....rrr......",
        "....rrrrr.....",
        ".....GgG......",
        ".....GGG......",
        "......G.......",
        "..............",
        "..............",
        "..............",
    ])


def icon_clear() -> Image.Image:
    return _from_rows([
        "..............",
        "....ssssss....",
        "...sSSSSSSs...",
        "...ssssssss...",
        "...swwwwwws...",
        "...swxwwxws...",
        "...swwxwxws...",
        "...swwwxwws...",
        "...swwxwxws...",
        "...swxwwxws...",
        "...swwwwwws...",
        "...ssssssss...",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_launch() -> Image.Image:
    return _from_rows([
        ".......w......",
        "......www.....",
        "......wsw.....",
        ".....wsssw....",
        ".....wcccw....",
        ".....wcccw....",
        ".....wcccw....",
        "....wwcccww...",
        "...w.wcccw.w..",
        "..w..woow..w..",
        ".....woYow....",
        "......oYo.....",
        "......aOa.....",
        ".......O......",
        "..............",
        "..............",
    ])


def icon_trap() -> Image.Image:
    return _from_rows([
        "..............",
        "..s......s....",
        "..ss....ss....",
        "...ss..ss.....",
        "....ssss......",
        ".....ss.......",
        "..oooooooo....",
        ".oOOOOOOOOo...",
        ".oOaaaaaaOo...",
        ".oOOOOOOOOo...",
        ".nnnnnnnnnn...",
        ".nnnnnnnnnn...",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_chicken() -> Image.Image:
    return _from_rows([
        "..............",
        ".....rr.......",
        "....rrr.......",
        "...llllll.....",
        "..llllllll....",
        "..lwwlllll....",
        "..ldwllllooo..",
        "..lllllloYo...",
        "...llllloo....",
        "...llllll.....",
        "....rrrr......",
        ".....rr.......",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_anvil() -> Image.Image:
    return _from_rows([
        "..............",
        ".sssssssssss..",
        ".swwwwwwwwws..",
        ".sssssssssss..",
        "...sSSSSSs....",
        "...sSSSSSs....",
        "...sSSSSSs....",
        "..sSSSSSSSs...",
        ".sSSSSSSSSSs..",
        ".sddddddddds..",
        ".sssssssssss..",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_storm() -> Image.Image:
    return _from_rows([
        "..............",
        "....sss.......",
        "...sssss.ss...",
        "..sswwsssss...",
        ".sswwwwwwss...",
        ".ssssssssss...",
        "......y.......",
        ".....yY.......",
        "....yYy.......",
        ".....yY.......",
        "......y.......",
        ".....y........",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_hype() -> Image.Image:
    return _from_rows([
        "..............",
        "......Y.......",
        ".....YyY......",
        "....YyoyY.....",
        "....YoooY.....",
        "...YyoaoY.....",
        "...YooooY.....",
        "....YoaY......",
        "....YooY......",
        ".....RR.......",
        ".....RR.......",
        ".....RR.......",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_dome() -> Image.Image:
    return _from_rows([
        "..............",
        ".....vvv......",
        "....vwwvv.....",
        "...vwwvvvv....",
        "..vvvVVvvvv...",
        "..vvVVVVvvv...",
        ".vvVVVVVVvv...",
        ".vvVVVVVVvv...",
        ".vvvvvvvvvv...",
        ".VVVVVVVVVV...",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_lightning() -> Image.Image:
    return _from_rows([
        "..............",
        "......YYYY....",
        ".....YYYy.....",
        "....YYyy......",
        "...YYyy.......",
        "..YYYYYYY.....",
        ".....YYy......",
        "....YYy.......",
        "...YYy........",
        "..YYy.........",
        ".YYy..........",
        ".Yy...........",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_phantom() -> Image.Image:
    return _from_rows([
        "..............",
        "c..........c..",
        "cc........cc..",
        ".cccccccccc...",
        "..cccccccc....",
        "...ccwwcc.....",
        "...ccdcc......",
        "....cccc......",
        ".....cc.......",
        ".....CC.......",
        ".....CC.......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_wheel() -> Image.Image:
    return _from_rows([
        "..............",
        "....ryvbg.....",
        "...rryybbg....",
        "..rrryyybbg...",
        ".rrrryYyybbg..",
        ".rrrrykyybbg..",
        ".vvvvvkvvooo..",
        ".vvvvvkvvooo..",
        ".vvvv.k.oooo..",
        "..vvv...ooo...",
        "...vv...oo....",
        "....v...o.....",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_lava() -> Image.Image:
    return _from_rows([
        "..............",
        "....aaaaaa....",
        "...aaooYooa...",
        "...aoYYYYOa...",
        "...SSooooSS...",
        "...S......S...",
        "...S.aaaa.S...",
        "...S.aYYa.S...",
        "...S.aaaa.S...",
        "...SSSSSSSS...",
        "....SSSSSS....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_pit() -> Image.Image:
    return _from_rows([
        "..............",
        ".nn..gg..nn...",
        ".nnnnnnnnnn...",
        ".n........n...",
        ".n.dddddd.n...",
        ".n.dddddd.n...",
        ".n.dkkkkd.n...",
        ".n.dkkkkd.n...",
        ".n.dddddd.n...",
        ".nnnnnnnnnn...",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_drop() -> Image.Image:
    return _from_rows([
        "....ssssss....",
        "...sSSSSSSs...",
        "...ssssssss...",
        "....s....s....",
        ".....s..s.....",
        "......ss......",
        "....yy..bb....",
        "...yYYy.bBb...",
        "..ggg....rrr..",
        ".ggGg...rRr...",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_titan() -> Image.Image:
    return _from_rows([
        "..............",
        "....rrrr......",
        "...rrwwrr.....",
        "...rrddrr.....",
        "....rrrr......",
        "..rrrrrrrr....",
        ".rr.rrrr.rr...",
        ".rr.rrrr.rr...",
        "....rrrr......",
        "....r..r......",
        "....r..r......",
        "....r..r......",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_ant() -> Image.Image:
    return _from_rows([
        "N.N...........",
        ".N............",
        ".UU...........",
        "UyyUU.........",
        "UdU.UUUU......",
        ".UU....Uu.....",
        "N.N...N.Uu....",
        "...N.N...U....",
        "....N...N.....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_shuffle() -> Image.Image:
    return _from_rows([
        "..............",
        "......vvv.....",
        ".......vv.....",
        "vvvvvv.vv.....",
        ".......v......",
        "..............",
        "......g.......",
        "....gg.gggggg.",
        "....gg........",
        ".....ggg......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_tnt() -> Image.Image:
    return _from_rows([
        "......oY......",
        "......oo......",
        ".....nnn......",
        "...rrrrrrr....",
        "..rrrrrrrrr...",
        "..rrwwwwwrr...",
        "..rrwrrrwrr...",
        "..rrwrrrwrr...",
        "..rrwwwwwrr...",
        "..rrrrrrrrr...",
        "...rrrrrrr....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_blind() -> Image.Image:
    return _from_rows([
        "..............",
        "..www....www..",
        ".wwwww..wwwww.",
        ".wwdww..wwdww.",
        ".wwwww..wwwww.",
        "..www....www..",
        "...x......x...",
        "..x.x....x.x..",
        ".x...x..x...x.",
        "..x.x....x.x..",
        "...x......x...",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_goldrush() -> Image.Image:
    return _from_rows([
        "..............",
        ".....yyyyyy...",
        "....yYYYYYYy..",
        "....yyyyyyyy..",
        "..yyyyyy.yyyy.",
        "..yYYYYy.yYYy.",
        "..yyyyyy.yyyy.",
        "yyyyyyyyyyyyy.",
        "yYYYYYYYYYYYy.",
        "yyyyyyyyyyyyy.",
        ".ee...........",
        "eee...........",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_disco() -> Image.Image:
    return _from_rows([
        "......ss......",
        "......ss......",
        "....cccccc....",
        "...cpcycvc....",
        "..ccycpcycc...",
        "..cvcycpcvc...",
        "..cpcycycpc...",
        "..ccvcpcvcc...",
        "...cycvcyc....",
        "....cccccc....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_treasure() -> Image.Image:
    return _from_rows([
        "..............",
        "......yy......",
        ".....yYYy.....",
        "...nnnnnnnn...",
        "..nnNNNNNNnn..",
        "..nnyyyyyynn..",
        "..nnnnYYnnnn..",
        "..nnyyyyyynn..",
        "..nnYYYYYYnn..",
        "..nnnnnnnnnn..",
        "..nnnnnnnnnn..",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_raid() -> Image.Image:
    return _from_rows([
        "..............",
        "r..........r..",
        "rr........rr..",
        ".rr.ssss.rr...",
        "..rrssssrr....",
        "...rssssr.....",
        "...ssssss.....",
        "...rssssr.....",
        "..rrssssrr....",
        ".rr.ssss.rr...",
        "rr........rr..",
        "r..........r..",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_heal() -> Image.Image:
    return _from_rows([
        "..............",
        "....gg..gg....",
        "...gGGg.gGGg..",
        "..gGGGggGGGg..",
        "..gGGGGGGGGg..",
        "...gGGwGGGg...",
        "...gGGGGGGg...",
        "....gGGGGg....",
        ".....gGGg.....",
        "......gg......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_diamond() -> Image.Image:
    return _from_rows([
        "..............",
        "......cc......",
        ".....cCCc.....",
        "....cCCCCc....",
        "...ccCwCCcc...",
        "...cCCCCCCc...",
        "....cCCCCc....",
        ".....cCCc.....",
        "......cc......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_water() -> Image.Image:
    return _from_rows([
        "..............",
        ".....bb.......",
        "....bBBb......",
        "....bBBb......",
        ".....bb.......",
        "...bb..bb.....",
        "..bBBb.bBBb...",
        "..bBBb.bBBb...",
        "...bb...bb....",
        "..............",
        "....bbbbbb....",
        "...bBBBBBBb...",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_fire() -> Image.Image:
    return _from_rows([
        "..............",
        "......y.......",
        ".....yYy......",
        "....yYoYy.....",
        "....yoooY.....",
        "...YyoooaY....",
        "...YooooY.....",
        "....YoaY......",
        "....YaaY......",
        ".....aa.......",
        ".....RR.......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_zombie() -> Image.Image:
    return _from_rows([
        "..............",
        "....gggggg....",
        "...gggggggg...",
        "...gddggddg...",
        "...gddggddg...",
        "...gggggggg...",
        "...ggddddgg...",
        "...ggdggdgg...",
        "....gggggg....",
        ".....gggg.....",
        "....g....g....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_sheep() -> Image.Image:
    return _from_rows([
        "..............",
        "....wwwwww....",
        "...wwwwwwww...",
        "...wwddwwdd...",
        "...wwwwwwww...",
        "...wwwllwww...",
        "....wwllww....",
        ".....llll.....",
        ".....w..w.....",
        ".....w..w.....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_coin() -> Image.Image:
    return _from_rows([
        "..............",
        ".....yyyy.....",
        "....yYYYYy....",
        "...yYYyyYYy...",
        "...yYywwyYy...",
        "...yYYyyYYy...",
        "....yYYYYy....",
        ".....yyyy.....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_creeper() -> Image.Image:
    return _from_rows([
        "..............",
        "...gggggggg...",
        "..gggggggggg..",
        "..ggddggddgg..",
        "..ggddggddgg..",
        "..gggggggggg..",
        "..gggddddggg..",
        "..ggddggddgg..",
        "..ggddggddgg..",
        "...gggggggg...",
        "....gg..gg....",
        "....gg..gg....",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_tp() -> Image.Image:
    return _from_rows([
        "..............",
        ".....vvvv.....",
        "....vVVVVv....",
        "...vVwVwVVv...",
        "...vVVVVVVv...",
        "...vVwwwVVv...",
        "....vVVVVv....",
        ".....vvvv.....",
        "......cc......",
        ".....cCCc.....",
        "......cc......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_morph() -> Image.Image:
    return _from_rows([
        "..............",
        "...ggg.llll...",
        "..gggg.lllll..",
        "..gddg.ldldl..",
        "..gggg.lllll..",
        "..gddg.lllll..",
        "...ggg.llll...",
        ".....vvvv.....",
        "....vVVVVv....",
        ".....vvvv.....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_tether() -> Image.Image:
    return _from_rows([
        "..............",
        "gg..........rr",
        "gGg........rRr",
        ".g.ssssssss.r.",
        "...sSSSSSSs...",
        "...ssssssss...",
        ".g.ssssssss.r.",
        "gGg........rRr",
        "gg..........rr",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_sub() -> Image.Image:
    return _from_rows([
        "..............",
        "y.y.y.y.y.y...",
        "yyyyyyyyyyy...",
        "yYYYYYYYYYy...",
        "yyyyyyyyyyy...",
        "..yYYYYYy.....",
        "..yyyyyyy.....",
        "...yYYYy......",
        "...yyyyy......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_follow() -> Image.Image:
    return _from_rows([
        "..............",
        "..pp....pp....",
        ".pppp..pppp...",
        ".pppppppppp...",
        ".pppppppppp...",
        "..pppppppp....",
        "...pppppp.....",
        "....pppp......",
        ".....pp.......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_firstmsg() -> Image.Image:
    return _from_rows([
        "..............",
        "......Y.......",
        ".....YYY......",
        "Y...YYYYY...Y.",
        "YYYYYYYYYYYYY.",
        "Y...YYYYY...Y.",
        ".....YYY......",
        "......Y.......",
        ".....yYy......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_deny_shield() -> Image.Image:
    return _from_rows([
        "..............",
        "....ssssss....",
        "...syyyyyys...",
        "...syYYYYYs...",
        "...syYwwYys...",
        "...syYYYYYs...",
        "...syyyyyys...",
        "....syyyys....",
        ".....syys.....",
        "......ss......",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_deny_cd() -> Image.Image:
    return _from_rows([
        "..............",
        "....ssssss....",
        "...s......s...",
        "..s..yyyy..s..",
        "..s..y..y..s..",
        "..s..y..y..s..",
        "..s.....y..s..",
        "...s......s...",
        "....ssssss....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_deny_hold() -> Image.Image:
    return _from_rows([
        "..............",
        "..yy....ss....",
        ".yYYy..sSSs...",
        ".yyyy..ssss...",
        ".y..y..s..s...",
        ".yyyy..ssss...",
        "..............",
        "....yyyyyy....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


def icon_deny_evil() -> Image.Image:
    return _from_rows([
        "..............",
        "....ssssss....",
        "...swwwwwws...",
        "...swddwdds...",
        "...swwwwwws...",
        "...s.wwww.s...",
        "....ssssss....",
        ".....rrrr.....",
        "....rrrrrr....",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    ])


WHEEL_ICON_BUILDERS = {
    "meteor": icon_meteor,
    "wither": icon_wither,
    "army": icon_army,
    "tntrain": icon_tntrain,
    "boss": icon_boss,
    "dragon": icon_dragon,
    "prison": icon_prison,
    "creeperstorm": icon_creeperstorm,
    "event": icon_event,
    "spin": icon_spin,
    "curse": icon_curse,
    "clear": icon_clear,
    "launch": icon_launch,
    "trap": icon_trap,
    "chicken": icon_chicken,
    "anvil": icon_anvil,
    "storm": icon_storm,
    "hype": icon_hype,
    "dome": icon_dome,
    "lightning": icon_lightning,
    "phantom": icon_phantom,
    "wheel": icon_wheel,
    "lava": icon_lava,
    "pit": icon_pit,
    "drop": icon_drop,
    "titan": icon_titan,
    "ant": icon_ant,
    "shuffle": icon_shuffle,
    "tnt": icon_tnt,
    "blind": icon_blind,
    "goldrush": icon_goldrush,
    "disco": icon_disco,
    "treasure": icon_treasure,
    "raid": icon_raid,
    "heal": icon_heal,
    "diamond": icon_diamond,
    "water": icon_water,
    "fire": icon_fire,
    "zombie": icon_zombie,
    "sheep": icon_sheep,
    "coin": icon_coin,
    "creeper": icon_creeper,
    "tp": icon_tp,
    "morph": icon_morph,
    "tether": icon_tether,
    "sub": icon_sub,
    "follow": icon_follow,
    "firstmsg": icon_firstmsg,
    "deny_shield": icon_deny_shield,
    "deny_cd": icon_deny_cd,
    "deny_hold": icon_deny_hold,
    "deny_evil": icon_deny_evil,
}

LABELS = {
    "meteor": "Метеор",
    "wither": "Визер",
    "army": "Легион",
    "tntrain": "TNT дождь",
    "boss": "Босс",
    "dragon": "Дракон",
    "prison": "Тюрьма",
    "creeperstorm": "Крипер",
    "event": "Событие",
    "spin": "Вихрь",
    "curse": "Проклятье",
    "clear": "Очистка",
    "launch": "Ракета",
    "trap": "Ловушка",
    "chicken": "Курица",
    "anvil": "Наковальня",
    "storm": "Шторм",
    "hype": "Хайп",
    "dome": "Купол",
    "lightning": "Молния",
    "phantom": "Фантом",
    "wheel": "Колесо",
    "lava": "Лава",
    "pit": "Яма",
    "drop": "Дроп",
    "titan": "Титан",
    "ant": "Муравей",
    "shuffle": "Хотбар",
    "tnt": "TNT",
    "blind": "Слепота",
    "goldrush": "Золото",
    "disco": "Диско",
    "treasure": "Сундук",
    "raid": "Рейд",
    "heal": "Хил",
    "diamond": "Алмаз",
    "water": "Вода",
    "fire": "Огонь",
    "zombie": "Зомби",
    "sheep": "Овца",
    "coin": "Монета",
    "creeper": "Крипер",
    "tp": "ТП",
    "morph": "Морф",
    "tether": "Связь",
    "sub": "Саб",
    "follow": "Фоллоу",
    "firstmsg": "Первое",
    "deny_shield": "Щит",
    "deny_cd": "КД",
    "deny_hold": "Держись",
    "deny_evil": "Блок",
}


def write_java(path: Path) -> None:
    for name, maker in WHEEL_ICON_BUILDERS.items():
        maker()
    lines = [
        "package net.twitchcraft.client;",
        "",
        "/** Спрайты 16×16 — копия tools/wheel_pixel_art.py, не править руками. */",
        "final class WheelIconSprites {",
        "    static final int GRID = 16;",
        "",
        "    private WheelIconSprites() {}",
        "",
        "    static String[] rows(String id) {",
        "        return switch (id) {",
    ]
    for name in WHEEL_ICON_BUILDERS:
        rows = SPRITE_ROWS[name]
        joined = ", ".join(f'"{r}"' for r in rows)
        lines.append(f'            case "{name}" -> new String[]{{{joined}}};')
    lines.append('            default -> rows("event");')
    lines.append("        };")
    lines.append("    }")
    lines.append("")
    lines.append("    static int color(char ch) {")
    lines.append("        return switch (ch) {")
    for ch, rgba in P.items():
        hexcol = (rgba[0] << 16) | (rgba[1] << 8) | rgba[2]
        lines.append(f"            case '{ch}' -> 0x{hexcol:06X};")
    lines.append("            default -> 0;")
    lines.append("        };")
    lines.append("    }")
    lines.append("}")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")
    print(f"  java {path}")


def write_preview(path: Path) -> None:
    names = list(WHEEL_ICON_BUILDERS)
    cols = 6
    rows = (len(names) + cols - 1) // cols
    pad = 12
    cell = CARD + 28
    img = Image.new("RGBA", (pad * 2 + cols * cell, pad * 2 + rows * cell), (18, 14, 22, 255))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 12)
    except OSError:
        font = ImageFont.load_default()
    for i, name in enumerate(names):
        icon = WHEEL_ICON_BUILDERS[name]()
        cx = pad + (i % cols) * cell
        cy = pad + (i // cols) * cell
        draw.rectangle((cx, cy, cx + CARD + 7, cy + CARD + 7), fill=(8, 6, 10, 255))
        img.paste(icon, (cx + 4, cy + 4), icon)
        draw.text((cx + 4, cy + CARD + 8), LABELS.get(name, name), fill=(240, 230, 200, 255), font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG")
    print(f"  preview {path} {img.size[0]}x{img.size[1]}")


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    write_preview(root / "tools" / "wheel_preview.png")
    write_java(root / "src" / "client" / "java" / "net" / "twitchcraft" / "client" / "WheelIconSprites.java")
