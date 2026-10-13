#!/usr/bin/env python3
"""Fetch icons from IconBuddy and convert them to Android vector drawables.

Reads `iconbuddy.json` (semantic key -> "collection:name"), stores each SVG in
`.iconbuddy/icons/<key>.svg` and writes `androidApp/src/main/res/drawable/ic_<key>.xml`.
Needs ICONBUDDY_API_KEY in the environment. Stroke icons get `stroke_width` from
the manifest style (default 2) so the whole set keeps one weight.
"""
import json, os, re, sys, urllib.request, xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MANIFEST = os.path.join(ROOT, "iconbuddy.json")
SVG_DIR = os.path.join(ROOT, ".iconbuddy", "icons")
RES_DIR = os.path.join(ROOT, "androidApp", "src", "main", "res", "drawable")
NS = "{http://www.w3.org/2000/svg}"


def fetch(icon_id):
    collection, name = icon_id.split(":", 1)
    req = urllib.request.Request(f"https://iconbuddy.com/api/v1/icons/{collection}/{name}",
                                 headers={"X-IconBuddy-Key": os.environ["ICONBUDDY_API_KEY"], "User-Agent": "nxplayer-iconbuddy-sync/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)["icon"]


def num(v, default=0.0):
    return float(v) if v not in (None, "") else default


def shape_to_path(tag, a):
    if tag == "path":
        return a.get("d", "")
    if tag == "circle":
        cx, cy, r = num(a.get("cx")), num(a.get("cy")), num(a.get("r"))
        return f"M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0"
    if tag == "ellipse":
        cx, cy, rx, ry = num(a.get("cx")), num(a.get("cy")), num(a.get("rx")), num(a.get("ry"))
        return f"M{cx - rx},{cy}a{rx},{ry} 0 1,0 {2 * rx},0a{rx},{ry} 0 1,0 {-2 * rx},0"
    if tag == "rect":
        x, y, w, h = num(a.get("x")), num(a.get("y")), num(a.get("width")), num(a.get("height"))
        rx = num(a.get("rx"), num(a.get("ry")))
        ry = num(a.get("ry"), rx)
        if rx == 0:
            return f"M{x},{y}h{w}v{h}h{-w}z"
        return (f"M{x + rx},{y}h{w - 2 * rx}a{rx},{ry} 0 0 1 {rx},{ry}v{h - 2 * ry}a{rx},{ry} 0 0 1 {-rx},{ry}"
                f"h{-(w - 2 * rx)}a{rx},{ry} 0 0 1 {-rx},{-ry}v{-(h - 2 * ry)}a{rx},{ry} 0 0 1 {rx},{-ry}z")
    if tag == "line":
        return f"M{a.get('x1', 0)},{a.get('y1', 0)}L{a.get('x2', 0)},{a.get('y2', 0)}"
    if tag in ("polyline", "polygon"):
        pts = re.findall(r"-?[\d.]+(?:e-?\d+)?", a.get("points", ""))
        pairs = [f"{pts[i]},{pts[i + 1]}" for i in range(0, len(pts) - 1, 2)]
        return "M" + "L".join(pairs) + ("z" if tag == "polygon" else "")
    return None


def to_vector(svg, stroke_width):
    root = ET.fromstring(svg)
    vb = [float(v) for v in root.get("viewBox", "0 0 24 24").split()]
    out = []

    def walk(el, inherited):
        attrs = dict(inherited)
        for k in ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule", "opacity", "fill-opacity"):
            if el.get(k) is not None:
                attrs[k] = el.get(k)
        tag = el.tag.replace(NS, "")
        d = shape_to_path(tag, el.attrib)
        if d:
            fill = attrs.get("fill", "currentColor")
            stroke = attrs.get("stroke", "none")
            p = [f'    <path android:pathData="{d}"']
            if fill != "none":
                p.append('        android:fillColor="#FFFFFFFF"')
                if attrs.get("fill-rule") == "evenodd":
                    p.append('        android:fillType="evenOdd"')
                alpha = attrs.get("fill-opacity", attrs.get("opacity"))
                if alpha:
                    p.append(f'        android:fillAlpha="{alpha}"')
            if stroke != "none":
                sw = stroke_width if stroke_width is not None else attrs.get("stroke-width", "1")
                p.append('        android:strokeColor="#FFFFFFFF"')
                p.append(f'        android:strokeWidth="{sw}"')
                p.append(f'        android:strokeLineCap="{attrs.get("stroke-linecap", "butt")}"')
                p.append(f'        android:strokeLineJoin="{attrs.get("stroke-linejoin", "miter")}"')
            out.append("\n".join(p) + " />")
        for child in el:
            walk(child, attrs)

    walk(root, {})
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp" android:height="24dp"\n'
        f'    android:viewportWidth="{vb[2]:g}" android:viewportHeight="{vb[3]:g}">\n'
        + "\n".join(out) + "\n</vector>\n"
    )


def main():
    manifest = json.load(open(MANIFEST))
    stroke_width = manifest.get("style", {}).get("stroke_width")
    only = set(sys.argv[1:])
    os.makedirs(SVG_DIR, exist_ok=True)
    os.makedirs(RES_DIR, exist_ok=True)
    for key, entry in manifest["icons"].items():
        if only and key not in only:
            continue
        icon_id = entry["id"] if isinstance(entry, dict) else entry
        try:
            icon = fetch(icon_id)
        except Exception as e:
            print(f"FAIL {key} {icon_id}: {e}")
            continue
        open(os.path.join(SVG_DIR, f"{key}.svg"), "w").write(icon["svg"])
        open(os.path.join(RES_DIR, f"ic_{key}.xml"), "w").write(to_vector(icon["svg"], stroke_width))
        manifest["icons"][key] = {"id": icon_id, "file": f".iconbuddy/icons/{key}.svg", "license": icon.get("license")}
        print(f"ok   {key:18} {icon_id}")
    json.dump(manifest, open(MANIFEST, "w"), indent=2)


if __name__ == "__main__":
    main()
