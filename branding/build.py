# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "fonttools>=4.60",
#     "uharfbuzz>=0.50",
#     "resvg-py>=0.5",
#     "pillow>=12",
# ]
# ///
"""Build the Plex brand assets for Plex and every module repository.

Run from the Plex repository root:

    uv run branding/build.py

All text is converted to outline paths, so the SVG files need no fonts to render.
"""

import io
import tomllib
from pathlib import Path

import resvg_py
import uharfbuzz as hb
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from PIL import Image

HERE = Path(__file__).resolve().parent
PROJECTS = HERE.parent.parent
FONT_PATH = HERE / "fonts" / "Archivo[wdth,wght].ttf"

# The "tile cut" mark on its 32-unit grid: the stepped-bowl P cut out of a solid tile. Never change this geometry.
MARK = "M2 2H30V30H12V22H22V18H26V10H22V6H6V30H2ZM12 12H20V16H12Z"
MARK_LEFT, MARK_TOP, MARK_WIDTH, MARK_HEIGHT = 2, 2, 28, 28
STEP = 4

BACKGROUND = "#121010"
DOT = "#2A2523"
ACCENT = "#FF5A1F"
TEXT = "#FAF6F3"
MUTED = "#8F8580"

HEADLINE = {"wght": 800, "wdth": 125}
HEADLINE_TRACKING = -0.02
TAGLINE = {"wght": 400, "wdth": 100}
SUBTITLE = {"wght": 600, "wdth": 100}

# Module names, set by main(). Every module card uses one subtitle size, the size at which the longest name fits.
MODULE_NAMES = []

font = TTFont(FONT_PATH)
UNITS_PER_EM = font["head"].unitsPerEm
hb_font = hb.Font(hb.Face(FONT_PATH.read_bytes()))


def number(value):
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    return "0" if text == "-0" else text


def shape(text, style, size, tracking=0.0):
    """Shape one line. Glyph positions and the ink box are in pixels, with the baseline at y = 0."""
    hb_font.set_variations(style)
    buffer = hb.Buffer()
    buffer.add_str(text)
    buffer.guess_segment_properties()
    hb.shape(hb_font, buffer)
    glyph_set = font.getGlyphSet(location=style)
    scale = size / UNITS_PER_EM
    glyphs = []
    pen_x = 0.0
    bounds = BoundsPen(glyph_set)
    for info, position in zip(buffer.glyph_infos, buffer.glyph_positions):
        name = font.getGlyphName(info.codepoint)
        x = pen_x + position.x_offset * scale
        y = -position.y_offset * scale
        glyphs.append((name, x, y, text[info.cluster]))
        glyph_set[name].draw(TransformPen(bounds, (scale, 0, 0, -scale, x, y)))
        pen_x += position.x_advance * scale + tracking * size
    return {"glyphs": glyphs, "glyph_set": glyph_set, "scale": scale, "ink": bounds.bounds}


def ink_width(line):
    left, _, right, _ = line["ink"]
    return right - left


def cap_height(style, size):
    return -shape("H", style, size)["ink"][1]


def draw(line, left, baseline, color):
    """Return an SVG path for a shaped line with its ink starting at x = left."""
    pen = SVGPathPen(line["glyph_set"], ntos=number)
    offset = left - line["ink"][0]
    scale = line["scale"]
    for name, x, y, _ in line["glyphs"]:
        line["glyph_set"][name].draw(TransformPen(pen, (scale, 0, 0, -scale, offset + x, baseline + y)))
    commands = pen.getCommands()
    return f'<path fill="{color}" d="{commands}"/>' if commands else ""


def split_tagline(text):
    words = text.split()
    candidates = [[" ".join(words[:i]), " ".join(words[i:])] for i in range(1, len(words))]
    return min(candidates, key=lambda lines: max(len(line) for line in lines))


def fit(text, style, max_size, min_size, max_width, tracking, split):
    """Pick the largest size up to max_size that fits max_width on one line, else wrap with split."""
    width = ink_width(shape(text, style, max_size, tracking))
    size = min(max_size, max_size * max_width / width)
    lines = [text]
    if size < min_size:
        lines = split(text)
        width = max(ink_width(shape(line, style, max_size, tracking)) for line in lines)
        size = min(max_size, max_size * max_width / width)
    return [shape(line, style, size, tracking) for line in lines], size


def mark(left, top, scale, fill=ACCENT):
    x = left - MARK_LEFT * scale
    y = top - MARK_TOP * scale
    return f'<path fill="{fill}" fill-rule="evenodd" transform="translate({number(x)} {number(y)}) scale({number(scale)})" d="{MARK}"/>'


def step_grid(width, height, left, top, pitch, size):
    """Faint square dots on the mark's 4-unit step lattice, aligned with the mark's corners."""
    x = left % pitch
    y = top % pitch
    return (
        f'<pattern id="steps" width="{pitch}" height="{pitch}" patternUnits="userSpaceOnUse" '
        f'x="{number(x - size / 2)}" y="{number(y - size / 2)}">'
        f'<rect width="{size}" height="{size}" fill="{DOT}"/></pattern>'
        f'<rect width="{width}" height="{height}" fill="url(#steps)"/>'
    )


def svg(width, height, body, rounded=0):
    background = f'<rect width="{width}" height="{height}" rx="{rounded}" fill="{BACKGROUND}"/>'
    clip_open = clip_close = ""
    if rounded:
        clip_open = f'<clipPath id="frame"><rect width="{width}" height="{height}" rx="{rounded}"/></clipPath><g clip-path="url(#frame)">'
        clip_close = "</g>"
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}">'
        f"{background}{clip_open}{body}{clip_close}</svg>\n"
    )


def subtitle(module, size):
    """The module subtitle: the module name, smaller than the wordmark."""
    name = shape(module, SUBTITLE, size)
    return {"name": name, "width": ink_width(name)}


def subtitle_size(max_size, max_width):
    """One subtitle size for every module card: the largest size at which the longest subtitle fits max_width."""
    widest = max(subtitle(name, max_size)["width"] for name in MODULE_NAMES)
    return min(max_size, max_size * max_width / widest)


def text_block(module, tagline, head_cap, sub_size, tag_size, max_width):
    """Lay out the Plex wordmark, the optional module subtitle, and the tagline.

    Returns each line with its baseline offset from the top of the block, plus the block width and height.
    """
    head = shape("Plex", HEADLINE, head_cap / cap_height(HEADLINE, 1), HEADLINE_TRACKING)
    tag_lines, tag_size = fit(tagline, TAGLINE, tag_size, tag_size * 0.85, max_width, 0.0, split_tagline)
    tag_cap = cap_height(TAGLINE, tag_size)
    lines = [("head", head, head_cap)]
    widths = [ink_width(head)]
    baseline = head_cap
    if module:
        sub = subtitle(module, subtitle_size(sub_size, max_width))
        sub_cap = cap_height(SUBTITLE, sub["name"]["scale"] * UNITS_PER_EM)
        baseline += sub_cap + head_cap * 0.4
        lines.append(("sub", sub, baseline))
        widths.append(sub["width"])
        baseline += tag_cap + sub_cap * 0.9
    else:
        # With the Plex wordmark alone, this puts the tagline baseline on the mark's bottom edge.
        baseline += max(head_cap / 2, tag_cap + 40)
    for index, line in enumerate(tag_lines):
        lines.append(("tag", line, baseline + index * tag_size * 1.4))
        widths.append(ink_width(line))
    return {"lines": lines, "width": max(widths), "height": lines[-1][2]}


def draw_block(block, top, center):
    """Draw a text block with every line centered on x = center."""
    parts = []
    for kind, line, offset in block["lines"]:
        baseline = top + offset
        if kind == "sub":
            parts.append(draw(line["name"], center - line["width"] / 2, baseline, MUTED))
        else:
            parts.append(draw(line, center - ink_width(line) / 2, baseline, TEXT if kind == "head" else MUTED))
    return "".join(parts)


def hero(module, tagline):
    """1600x600 README banner: the mark at left, the Plex wordmark, module subtitle, and tagline beside it."""
    width, height, scale = 1600, 600, 8
    gap = 6 * scale
    margin = 112
    mark_width, mark_height = MARK_WIDTH * scale, MARK_HEIGHT * scale
    block = text_block(module, tagline, 14 * scale, 56, 38, width - 2 * margin - mark_width - gap)
    left = round((width - mark_width - gap - block["width"]) / 2)
    top = (height - mark_height) // 2
    body = (
        step_grid(width, height, left, top, STEP * scale, 3)
        + mark(left, top, scale)
        + draw_block(block, height / 2 - block["height"] / 2, center=left + mark_width + gap + block["width"] / 2)
    )
    return svg(width, height, body, rounded=24)


def social(module, tagline):
    """1280x640 social card: the hero layout (mark at left, text beside it) inside a 1040px central safe area."""
    width, height, scale = 1280, 640, 7
    gap = 6 * scale
    mark_width, mark_height = MARK_WIDTH * scale, MARK_HEIGHT * scale
    block = text_block(module, tagline, 14 * scale, 48, 32, 1040 - mark_width - gap)
    left = round((width - mark_width - gap - block["width"]) / 2)
    top = (height - mark_height) // 2
    body = (
        step_grid(width, height, left, top, STEP * scale, 3)
        + mark(left, top, scale)
        + draw_block(block, height / 2 - block["height"] / 2, center=left + mark_width + gap + block["width"] / 2)
    )
    return svg(width, height, body)


def avatar():
    """1024x1024 avatar: the tile centered; its corners stay inside a circle crop."""
    size, scale = 1024, 24
    left = (size - MARK_WIDTH * scale) // 2
    top = (size - MARK_HEIGHT * scale) // 2
    return svg(size, size, step_grid(size, size, left, top, STEP * scale, 4) + mark(left, top, scale))


def favicon():
    """The tile is its own icon. Every edge falls on a whole pixel at 16, 32, and 48 px."""
    return mark_only()


def mark_only():
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">'
        f'<path fill="{ACCENT}" fill-rule="evenodd" d="{MARK}"/></svg>\n'
    )


def render(source, width, height):
    return resvg_py.svg_to_bytes(svg_string=source, width=width, height=height, skip_system_fonts=True)


def write_text(path, content):
    path.write_text(content, encoding="utf-8", newline="\n")
    print(f"wrote {path}")


def write_bytes(path, content):
    path.write_bytes(content)
    print(f"wrote {path}")


def main():
    repos = tomllib.loads((HERE / "brand.toml").read_text(encoding="utf-8"))["repo"]
    MODULE_NAMES.extend(repo["module"] for repo in repos if "module" in repo)
    for repo in repos:
        out = PROJECTS / repo["folder"] / "branding"
        out.mkdir(exist_ok=True)
        module = repo.get("module")
        write_text(out / f"{repo['slug']}-hero.svg", hero(module, repo["tagline"]))
        write_bytes(out / f"{repo['slug']}-social.png", render(social(module, repo["tagline"]), 1280, 640))

    write_text(HERE / "plex-mark.svg", mark_only())
    avatar_svg = avatar()
    write_text(HERE / "plex-avatar.svg", avatar_svg)
    write_bytes(HERE / "plex-avatar.png", render(avatar_svg, 1024, 1024))
    favicon_svg = favicon()
    write_text(HERE / "favicon.svg", favicon_svg)
    sizes = [16, 32, 48]
    icons = [Image.open(io.BytesIO(render(favicon_svg, size, size))) for size in sizes]
    with io.BytesIO() as ico:
        icons[-1].save(ico, format="ICO", sizes=[(size, size) for size in sizes], append_images=icons[:-1])
        write_bytes(HERE / "favicon.ico", ico.getvalue())


if __name__ == "__main__":
    main()
