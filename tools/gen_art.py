"""Generates pet sprite sheets and burrow art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py            # everything missing
    python tools/gen_art.py mole bee   # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, warm autumn palette, cute and round, big shiny eyes."
)
# objects are plain things: the generator likes to give everything a cute face, so it is told twice not to
OBJ_STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, warm autumn palette. These are plain inanimate objects: NO faces, NO eyes, NO mouths, "
    "NO blush, nothing alive about them."
)
GRID = (
    "Layout: 3 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no grid lines, "
    "no shadows on the background, no other objects except those listed."
)

# id: (look, signature move)
PETS = {
    "hamster": ("a golden hamster with a white belly and a tiny red neckerchief", "stuffing sunflower seeds into hugely puffed cheeks"),
    "squirrel": ("a red squirrel with a big fluffy tail and a small green scarf", "carrying a little wicker basket full of acorns and an apple"),
    "raccoon": ("a raccoon with a small brown barista apron", "holding a tiny steaming coffee cup with both paws"),
    "beaver": ("a chubby beaver with a yellow builder's helmet", "carrying a wooden plank on its shoulder"),
    "mole": ("a velvety dark mole with pink paws, round glasses and a tiny train conductor cap", "holding up a small paper ticket, a little lantern in the other paw"),
    "hedgehog": ("a hedgehog with a tiny white cap with a red cross", "holding a shiny red apple and a small first-aid tin"),
    "magpie": ("a black and white magpie with a glossy blue-green tail", "carrying a small paper shopping bag in its beak"),
    "chipmunk": ("a striped chipmunk with a little party hat", "juggling three small colourful balls"),
    "bat": ("a small fluffy brown fruit bat with big ears and tiny headphones", "hanging upside down, holding a tiny glowing phone"),
    "mouse": ("a field mouse with a knitted cream shawl", "hugging a small teddy bear"),
    "bee": ("a fuzzy round bumblebee with a pink bow", "carrying a wrapped gift box with a ribbon"),
    "pika": ("a round grey pika with a small orange backpack", "holding open a paper map, walking"),
    "rabbit": ("a white rabbit with a pink towel turban", "combing one long ear in front of a small hand mirror"),
    "owl": ("a little brown owl with round glasses", "reading a thick open book"),
    "ant": ("a red ant with tiny boots", "carrying a golden coin bigger than itself over its head"),
    "dormouse": ("a hazel dormouse with orange fur and a striped nightcap", "rolling a big hazelnut"),
    "jay": ("a Eurasian jay with pinkish-brown feathers and bright blue wing patches", "pushing an acorn into a small hole in the ground"),
    "badger": ("a badger with a small miner's headlamp", "pushing a big grey boulder"),
    "marmot": ("a plump marmot standing upright", "looking through a small brass spyglass"),
    "fox": ("a red fox cub with a little blue scarf", "tiptoeing with a small glowing lantern, sly smile"),
    "crow": ("a glossy black crow with a gold monocle", "holding a shiny golden key in its beak"),
    "tortoise": ("a small tortoise with a tiny pink piggy bank tied on its shell", "walking slowly and steadily, determined"),
    "otter": ("a river otter with a sleek brown coat", "floating on its back holding a smooth pebble on its belly"),
    "pangolin": ("a pangolin whose scales look like shiny gold coins", "curled up into a gleaming golden ball, peeking out"),
}


def pet_prompt(desc, signature):
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy money-saving app pet that lives in a burrow pantry: {desc}.
{STYLE}
{GRID}
Cells, left to right, top to bottom:
1) baby: tiny, round, stubby limbs, very cute
2) teen: bigger, a bit more confident
3) adult: grown, plump and content, standing proud
4) adult {signature}
5) adult cheering, jumping with joy, paws (or wings) up in triumph
6) adult asleep, curled up, peaceful face
The same character and palette in every cell, facing three-quarters right."""


EGG_HINTS = [
    "golden with a white belly patch (hamster)", "rusty orange with a fluffy tail curl (squirrel)", "grey with a black mask stripe (raccoon)",
    "brown with wood-grain lines (beaver)", "dark velvet with pink speckles (mole)", "brown with a spiky top (hedgehog)",
    "black and white with a blue-green sheen (magpie)", "tan with dark stripes (chipmunk)", "dusk purple with small stars (bat)",
    "cream knitted pattern (mouse)", "yellow and black stripes, fuzzy (bee)", "soft grey with a small mountain pattern (pika)",
    "white with pink spots (rabbit)", "brown with feather scallops and round eye spots (owl)", "small glossy red (ant)",
    "orange with a cream nightcap stripe (dormouse)", "pinkish-brown with a bright blue band (jay)", "black and white with a grey band (badger)",
    "sandy brown with a little spyglass mark (marmot)", "red-orange with a white tip (fox)", "glossy black with a gold dot (crow)",
    "green hexagon shell pattern (tortoise)", "sleek brown with water ripples (otter)", "shiny gold with coin scales, glowing (pangolin)",
]

OTHER = {
    "eggs": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 24 pet eggs for a cozy money-saving app.
""" + OBJ_STYLE + """
Layout: 6 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines.
Each egg is the same size and egg-shaped, but with its own pattern and colours that hint at the creature inside, in this order (left to right, top to bottom):
""" + ", ".join(f"{i + 1} {h}" for i, h in enumerate(EGG_HINTS)) + ".",
    "burrow": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy money-saving app: the inside of a small cozy underground burrow pantry under an old oak, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: warm earthen walls with a few oak roots coming through the ceiling, a round wooden window high on the LEFT wall whose glass is filled with flat solid pure magenta (#FF00FF), a wooden plank floor with a round woven rug in the middle, warm lantern light.
IMPORTANT: keep the room mostly EMPTY: no furniture, no animals, no text. The centre of the floor and the walls on the right must be clear, free space (furniture will be added later).
Only small details: a few roots, a small round wooden door on the far right, a hook on the wall.""",
    "items": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 cozy burrow furniture objects for a money-saving app, each a separate object, seen from the front (side-on like a 2D game room).
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a wooden wall shelf with jars of jam and honey, 2 two stacked wooden barrels, 3 a cozy patchwork armchair, 4 a small cast-iron stove with a warm glow,
5 a tall bookshelf full of books, 6 a brass oil lantern hanging from a hook, 7 a potted fern, 8 a round wall clock,
9 a small wooden sled, 10 a pile of grain sacks, 11 a wooden abacus with coloured beads, 12 an iron-bound treasure chest, closed,
13 a string of dried mushrooms and herbs (horizontal), 14 a small hammock between two pegs, 15 a framed painting of an oak tree (no text), 16 a tiny desk with an ink pot, quill and an open ledger.""",
    "props": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 small objects for a cozy money-saving app, each separate, front view.
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a single shiny acorn, 2 a small heap of five acorns, 3 a golden coin with an acorn emblem, 4 a small pile of golden cookie crumbs,
5 an empty glass jam jar with a cloth lid, 6 a half-full jar of red jam with a cloth lid, 7 a full jar of red jam with a cloth lid and a ribbon, 8 a big round grey mossy boulder,
9 a bulging grain sack tied with string, 10 a wooden barrel with a tap, 11 a snowflake ice crystal, 12 a small wrapped gift box,
13 a speckled egg in a straw nest, 14 a rolled paper receipt, 15 a golden medal with an oak leaf, 16 a small treasure chest open with gold coins.""",
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
    p = image_of((r.stdout or "") + (r.stderr or ""))
    if p is None:
        print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = list(OTHER.items()) + [(k, pet_prompt(*v)) for k, v in PETS.items()]
    jobs = [j for j in jobs if not want or j[0] in want]
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
