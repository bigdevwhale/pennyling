"""Cuts the Codex sheets in art/sheets into app drawables.

    python tools/cut.py

Pets (3×2): pet_<id>_<0..5>.webp – baby, teen, adult, signature move, cheer, asleep.
Items (4×4): item_<id>.webp. Props (4×4): prop_<id>.webp. Eggs (6×4): egg_<species>.webp.
Burrow: burrow_bg.webp with the magenta window glass made see-through (the sky is painted behind it).
The magenta background is keyed out; each frame is cropped to its creature.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PETS = ["hamster", "squirrel", "raccoon", "beaver", "mole", "hedgehog", "magpie", "chipmunk", "bat", "mouse", "bee", "pika",
        "rabbit", "owl", "ant", "dormouse", "jay", "badger", "marmot", "fox", "crow", "tortoise", "otter", "pangolin"]
ITEMS = ["shelf", "barrels", "armchair", "stove", "books", "lantern", "fern", "clock",
         "sled", "sacks", "abacus", "chest", "garland", "hammock", "painting", "desk"]
PROPS = ["acorn", "acorns", "coin", "crumbs", "jar0", "jar1", "jar2", "boulder",
         "sack", "barrel", "snowflake", "gift", "egg", "receipt", "medal", "treasure"]
MAX = 320


def key(im):
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = min(r, b) - g  # how magenta
            if m > 90:
                px[x, y] = (0, 0, 0, 0)
            elif m > 40:
                # a fringe: pull the pink out and fade it
                k = (m - 40) / 50
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(255 * (1 - k)))
    return im


def border_clean(cell):
    """Drops bits of the neighbouring cells: anything opaque that touches the cell's edge."""
    import numpy as np
    from scipy import ndimage
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = max(range(1, n + 1), key=lambda k: sizes[k]) if n else 0
    for k in edge:
        if k != big:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(sheet, cols, rows):
    im = key(Image.open(sheet))
    w, h = im.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = border_clean(im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch))))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            s = MAX / max(cell.size)
            if s < 1:
                cell = cell.resize((max(1, round(cell.width * s)), max(1, round(cell.height * s))), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for p in PETS:
        f = SHEETS / f"{p}.png"
        if not f.exists():
            print("missing", p)
            continue
        for n, fr in enumerate(frames(f, 3, 2)):
            save(fr, f"pet_{p}_{n}")
        print("pet", p)
    for sheet, names, prefix in (("items", ITEMS, "item"), ("props", PROPS, "prop")):
        if (SHEETS / f"{sheet}.png").exists():
            for name, fr in zip(names, frames(SHEETS / f"{sheet}.png", 4, 4)):
                save(fr, f"{prefix}_{name}")
            print(sheet)
    if (SHEETS / "eggs.png").exists():
        for name, fr in zip(PETS, frames(SHEETS / "eggs.png", 6, 4)):
            save(fr, f"egg_{name}")
        print("eggs")
    if (SHEETS / "burrow.png").exists():
        g = key(Image.open(SHEETS / "burrow.png"))
        g = g.resize((1152, round(1152 * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / "burrow_bg.webp", "WEBP", quality=88, method=6)
        print("burrow")


if __name__ == "__main__":
    main()
