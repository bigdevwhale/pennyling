"""Glues rendered screens side by side (trimming the empty tail of tall pages) for a quick look.
    python tools/sheet.py out.png 1-home 2-log ...   (names from app/build/art/screens)"""
import sys
from pathlib import Path
from PIL import Image, ImageChops
D = Path(__file__).resolve().parent.parent / "app/build/art/screens"
def trim(im):
    bg = Image.new(im.mode, im.size, im.getpixel((5, im.height - 5)))
    box = ImageChops.difference(im, bg).getbbox()
    return im.crop((0, 0, im.width, min(im.height, (box[3] if box else im.height) + 40)))
ims = [trim(Image.open(D / f"{n}.png").convert("RGB")) for n in sys.argv[2:]]
W = 420; ims = [i.resize((W, round(i.height * W / i.width))) for i in ims]
out = Image.new("RGB", (W * len(ims) + 10 * (len(ims) - 1), max(i.height for i in ims)), "white")
for k, i in enumerate(ims): out.paste(i, (k * (W + 10), 0))
out.save(sys.argv[1])
