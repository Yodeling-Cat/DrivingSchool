"""Derives the launcher icon variants a system recolors from the full-color design sources.

Run it after replacing the sources in assets/design/app-icon/ (an IconKitchen export), then copy
the results into the platforms as AGENTS.md "App icon sources" describes:

    python tools/app-icon/derive_variants.py

It writes:

- android/res/mipmap-*/ic_launcher_monochrome.png: Android 13+ themed icons draw only this
  layer's alpha, in the wallpaper's color over a background of the wallpaper's color. The export's
  monochrome is a copy of the full-color foreground, so every pixel is opaque and the book comes out
  as one flat shape. Here the pages are fully opaque and the coral (the cover and the lines on the
  pages) is partly transparent, so the lines still show.
- ios/AppIcon-dark.png: the book on a transparent background, which iOS 18 lays over its own dark
  background when the home screen is dark.
- ios/AppIcon-tinted.png: the same book in grays on black, which iOS 18 colors with the user's
  tint: the lighter a pixel, the more of the tint it takes.

The design has two colors, a yellow page and a coral cover, and the script tells them apart by
their green channel, so a redesign in other colors needs the constants below changed.
"""

from collections import deque
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2] / "assets" / "design" / "app-icon"

# Green channel of the coral and the yellow; everything in the design is one or a blend of the two.
CORAL_GREEN = 90
YELLOW_GREEN = 198
# How opaque the coral is, as a share of the yellow, in the variants a system recolors.
CORAL_WEIGHT = 0.45
# How far into the book the anti-aliased edge against the white background reaches, in pixels.
EDGE = 3


def weight(rgb):
    """1 for the yellow page, CORAL_WEIGHT for the coral, and in between for a blend of the two."""
    t = (rgb[1] - CORAL_GREEN) / (YELLOW_GREEN - CORAL_GREEN)
    t = min(1.0, max(0.0, t))
    return CORAL_WEIGHT + (1 - CORAL_WEIGHT) * t


def extract_book(icon):
    """Takes the white background out of an opaque icon, keeping the book's anti-aliased edge.

    Returns the book as RGBA on transparency. The background is the white reachable from the
    corners. A pixel on the book's edge is a blend of white and the book color just inside it, so
    its alpha is how far it is from white towards that color, read on the blue channel, which is
    where white and both of the book's colors are furthest apart.
    """
    rgb = icon.convert("RGB")
    w, h = rgb.size
    marked = rgb.copy()
    sentinel = (0, 255, 0)
    for corner in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        ImageDraw.floodfill(marked, corner, sentinel, thresh=12)
    r, g, b = ImageChops.difference(marked, Image.new("RGB", (w, h), sentinel)).split()
    background = ImageChops.lighter(ImageChops.lighter(r, g), b).point(lambda v: 255 if v == 0 else 0)
    near_background = background.filter(ImageFilter.MaxFilter(2 * EDGE + 1))

    px = rgb.load()
    bg = background.load()
    near = near_background.load()

    def neighbors(x, y):
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h:
                yield nx, ny

    def is_edge(x, y):
        return near[x, y] and not bg[x, y]

    # The book color behind each edge pixel: spread the solid book's colors outwards over the edge.
    behind = {}
    queue = deque()
    for y in range(h):
        for x in range(w):
            if near[x, y]:
                continue
            for n in neighbors(x, y):
                if is_edge(*n) and n not in behind:
                    behind[n] = px[x, y]
                    queue.append(n)
    while queue:
        p = queue.popleft()
        for n in neighbors(*p):
            if is_edge(*n) and n not in behind:
                behind[n] = behind[p]
                queue.append(n)

    book = Image.new("RGBA", (w, h))
    out = book.load()
    for y in range(h):
        for x in range(w):
            if bg[x, y]:
                out[x, y] = (0, 0, 0, 0)
            elif not near[x, y]:
                out[x, y] = px[x, y] + (255,)
            else:
                ref = behind.get((x, y), px[x, y])
                alpha = (255 - px[x, y][2]) / max(1, 255 - ref[2])
                out[x, y] = ref + (round(255 * min(1.0, max(0.0, alpha))),)
    return book


def tinted(book):
    """The book in grays, lighter where the tint should be stronger, on black."""
    gray = Image.new("RGB", book.size, (0, 0, 0))
    src = book.load()
    out = gray.load()
    for y in range(book.height):
        for x in range(book.width):
            r, g, b, a = src[x, y]
            out[x, y] = (round(255 * weight((r, g, b)) * a / 255),) * 3
    return gray


def monochrome(foreground):
    """White, with the foreground's alpha scaled down where the design is coral."""
    mono = Image.new("RGBA", foreground.size)
    src = foreground.convert("RGBA").load()
    out = mono.load()
    for y in range(foreground.height):
        for x in range(foreground.width):
            r, g, b, a = src[x, y]
            out[x, y] = (255, 255, 255, round(a * weight((r, g, b))))
    return mono


def main():
    book = extract_book(Image.open(ROOT / "ios" / "AppIcon~ios-marketing.png"))
    book.save(ROOT / "ios" / "AppIcon-dark.png", optimize=True)
    tinted(book).save(ROOT / "ios" / "AppIcon-tinted.png", optimize=True)
    for density in sorted((ROOT / "android" / "res").glob("mipmap-*dpi")):
        foreground = Image.open(density / "ic_launcher_foreground.png")
        monochrome(foreground).save(density / "ic_launcher_monochrome.png", optimize=True)


if __name__ == "__main__":
    main()
