"""Draws the Bou mark: the letter ব in Anek Bangla whose headline (matra) is a voice waveform.

Idea: in Bengali script every word hangs from the matra. Here the matra is the speaker's voice,
so speaking literally writes the Bengali letter. Colours come from the website tokens.

Run: python make_logo.py <Anek Bangla ExtraBold 85% width .ttf> <out dir>
"""
import sys
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.ttLib import TTFont

FONT, OUT = sys.argv[1], Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)

VIOLET, VIOLET_2, VIOLET_DEEP, INK = "#7c3aed", "#9333ea", "#5b21b6", "#22104a"
MANGO, GROUND = "#f59e0b", "#f6f1ff"

font = TTFont(FONT)
glyphs = font.getGlyphSet()
cmap = font.getBestCmap()

MATRA_Y0, MATRA_Y1 = 1085, 1329  # the headline band in this font (font units, y up)


def body_path(char):
    """Glyph outline without its matra contours (any contour lying inside the headline band)."""
    rec = []
    pen = SVGPathPen(glyphs)
    glyphs[cmap[ord(char)]].draw(pen)
    # Split the path into contours and drop the ones that sit wholly in the matra band.
    from fontTools.pens.recordingPen import RecordingPen
    r = RecordingPen()
    glyphs[cmap[ord(char)]].draw(r)
    contours, cur = [], []
    for op, args in r.value:
        cur.append((op, args))
        if op in ("closePath", "endPath"):
            contours.append(cur)
            cur = []
    for c in contours:
        ys = [pt[1] for _, a in c for pt in a]
        if ys and min(ys) >= MATRA_Y0 - 5 and max(ys) <= MATRA_Y1 + 5:
            continue
        p = SVGPathPen(glyphs)
        for op, args in c:
            getattr(p, op)(*args)
        rec.append(p.getCommands())
    return " ".join(rec), glyphs[cmap[ord(char)]].width


def wave(x0, x1, settle, amp=230, length=620, width=176):
    """The matra drawn as one stroke: a spoken wave on the left that settles into the straight
    headline by x=settle, so the voice visibly becomes the Bengali letter."""
    import math
    cy = (MATRA_Y0 + MATRA_Y1) / 2
    pts = []
    x = x0
    while x <= x1:
        if x < settle:
            t = (x - x0) / (settle - x0)
            a = amp * (1 - t) ** 1.25
            y = cy + a * math.sin(2 * math.pi * (x - x0) / length)
        else:
            y = cy
        pts.append(f"{x:.0f} {y:.0f}")
        x += 8
    pts.append(f"{x1} {cy:.0f}")
    return (f'<path d="M{" L".join(pts)}" fill="none" stroke="currentColor" stroke-width="{width}" '
            f'stroke-linecap="round" stroke-linejoin="round"/>')


BA, BA_W = body_path("ব")
U, U_W = body_path("উ")


def glyph_group(paths, wave_svg, fill):
    return f'<g fill="{fill}" color="{fill}">{paths}{wave_svg}</g>'


def icon(tip):
    # Glyph box: x -30..980, y 0..~1440 (with wave peaks). Scale to sit optically centred.
    s = 0.40
    # box: wave from x -1020 to the letter's right edge 978, y 0..1500
    tx, ty = 512 - (-21 * s), 512 + (680 * s)
    dot = f'<circle cx="-150" cy="1235" r="70" fill="{MANGO}"/>' if tip else ""
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
<defs>
<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{VIOLET_2}"/></linearGradient>
<radialGradient id="glow" cx=".3" cy=".2" r=".9"><stop offset="0" stop-color="#ffffff" stop-opacity=".18"/><stop offset=".6" stop-color="#ffffff" stop-opacity="0"/></radialGradient>
</defs>
<rect width="1024" height="1024" rx="232" fill="url(#bg)"/>
<rect width="1024" height="1024" rx="232" fill="url(#glow)"/>
<g transform="translate({tx:.0f} {ty:.0f}) scale({s} {-s})">
{glyph_group(f'<path d="{BA}"/>', wave(-920, 873, 420), "#ffffff")}{dot}
</g>
</svg>'''


def wordmark():
    s = 0.12
    gap = 40
    total = BA_W + gap + U_W
    wave_svg = wave(-920, BA_W + gap + 1190, 420)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{(total + 1020) * s + 40:.0f}" height="{1900 * s:.0f}" viewBox="0 0 {(total + 1020) * s + 40:.0f} {1900 * s:.0f}">
<g transform="translate({20 + 1020 * s:.0f} {1840 * s:.0f}) scale({s} {-s})" fill="{INK}">
<path d="{BA}"/><path transform="translate({BA_W + gap} 0)" d="{U}"/>
<g color="{VIOLET}">{wave_svg}</g>
</g>
</svg>'''


def mark_group(s, cx, cy, color):
    """The mark alone (no background), scaled by s and centred on cx, cy."""
    tx, ty = cx - (-21 * s), cy + (680 * s)
    return (f'<g transform="translate({tx:.1f} {ty:.1f}) scale({s} {-s})">'
            f'{glyph_group(f'<path d="{BA}"/>', wave(-920, 873, 420), color)}</g>')


def fullbleed():
    """Android launcher art: square, no corners (the launcher masks it), mark inside the safe zone."""
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 1024 1024">
<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{VIOLET_2}"/></linearGradient></defs>
<rect width="1024" height="1024" fill="url(#bg)"/>{mark_group(0.25, 512, 512, "#ffffff")}
</svg>'''


def monochrome_vector():
    """Android 13+ themed icon: the mark in white on a 108dp canvas, inside the 66dp safe zone."""
    s = 0.25 * 108 / 1024
    tx, ty = 54 - (-21 * s), 54 + (680 * s)
    import math
    # the wave path in font units, reused as an Android stroke
    wave_d = wave(-920, 873, 420).split('d="')[1].split('"')[0]
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108.0dp" android:height="108.0dp"
    android:viewportWidth="108.0" android:viewportHeight="108.0">
    <!-- Bou mark: a spoken wave that becomes the matra of the letter ba (Material You themed icon) -->
    <group android:translateX="{tx:.3f}" android:translateY="{ty:.3f}" android:scaleX="{s:.5f}" android:scaleY="{-s:.5f}">
        <path android:fillColor="#ffffff" android:pathData="{BA}" />
        <path android:strokeColor="#ffffff" android:strokeWidth="176" android:strokeLineCap="round"
            android:strokeLineJoin="round" android:pathData="{wave_d}" />
    </group>
</vector>
'''


def social():
    """GitHub social preview, 1280x640."""
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="640" viewBox="0 0 1280 640">
<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{VIOLET_2}"/></linearGradient></defs>
<rect width="1280" height="640" fill="url(#bg)"/>{mark_group(0.16, 400, 318, "#ffffff")}
<g font-family="'Segoe UI', Arial, sans-serif" fill="#ffffff">
<text x="610" y="330" font-size="150" font-weight="700" letter-spacing="-4">Bou</text>
<text x="616" y="398" font-size="34" fill-opacity=".9">Bangla voice keyboard for Android</text>
</g>
</svg>'''


(OUT / "bou-icon-fullbleed.svg").write_text(fullbleed(), encoding="utf-8")
(OUT / "bou-monochrome.xml").write_text(monochrome_vector(), encoding="utf-8")
(OUT / "bou-social.svg").write_text(social(), encoding="utf-8")
(OUT / "bou-icon.svg").write_text(icon(False), encoding="utf-8")
(OUT / "bou-wordmark.svg").write_text(wordmark(), encoding="utf-8")
print("ok", OUT)
