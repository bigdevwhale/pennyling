"""The launcher icon: the acorn from the props sheet on the adaptive icon's safe zone, and docs/logo.png.

    python tools/icon.py
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from cut import frames, SHEETS  # noqa: E402

BG = (63, 94, 60, 255)  # icon_bg

acorn = frames(SHEETS / "props.png", 4, 4)[0]
# reload uncropped at full size: frames() limits to 320 px, which is plenty for 432
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
s = 250 / max(acorn.size)
a = acorn.resize((round(acorn.width * s), round(acorn.height * s)), Image.LANCZOS)
fg.paste(a, ((432 - a.width) // 2, (432 - a.height) // 2 + 6), a)
out = ROOT / "app/src/main/res/drawable-nodpi"
fg.save(out / "ic_launcher_fg.webp", "WEBP", lossless=True)
logo = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
m = Image.new("L", (512, 512), 0); ImageDraw.Draw(m).rounded_rectangle((0, 0, 511, 511), 112, fill=255)
bg = Image.new("RGBA", (512, 512), BG); logo.paste(bg, (0, 0), m)
big = fg.resize((512 * 432 // 330, 512 * 432 // 330), Image.LANCZOS)
logo.alpha_composite(big, ((512 - big.width) // 2, (512 - big.height) // 2))
(ROOT / "docs").mkdir(exist_ok=True)
logo.save(ROOT / "docs/logo.png")
print("icon ok")
