#!/usr/bin/env python3
"""Genera las variantes responsivas de las imágenes de frontend/public/img.

    python3 scripts/optimizar-imagenes.py        # requiere Pillow (pip install pillow)

Por cada foto original `nombre.webp` crea `nombre-640.webp`, `nombre-1280.webp`, `nombre-1920.webp` (las que sean
más angostas que el original) y una versión recomprimida al ancho original, `nombre-<ancho>.webp`. El original
queda solo como respaldo: el navegador nunca lo pide. Escribe frontend/src/lib/imagenesSitio.ts con las medidas
que usa el componente <ImagenSitio>. Es seguro repetirlo: ignora las variantes ya generadas y parte siempre del
original. Si se agrega una foto nueva, basta volver a correrlo.
"""
import json
import re
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent / "frontend"
IMG = RAIZ / "public" / "img"
ANCHOS = (640, 1280, 1920)
CALIDAD = 70

medidas = {}
for ruta in sorted(IMG.glob("*.webp")):
    if re.search(r"-(\d+)\.webp$", ruta.name):
        continue  # variante ya generada
    with Image.open(ruta) as im:
        ancho, alto = im.size
        medidas[ruta.stem] = {"ancho": ancho, "alto": alto, "variantes": []}
        for a in sorted({w for w in ANCHOS if w < ancho * 0.9} | {ancho}):
            destino = IMG / f"{ruta.stem}-{a}.webp"
            h = round(alto * a / ancho)
            base = im.convert("RGB")
            (base if a == ancho else base.resize((a, h), Image.LANCZOS)).save(destino, "WEBP", quality=CALIDAD, method=6)
            medidas[ruta.stem]["variantes"].append(a)
            print(f"{destino.name}: {destino.stat().st_size // 1024} KB")

salida = RAIZ / "src" / "lib" / "imagenesSitio.ts"
salida.write_text(
    "// Generado por scripts/optimizar-imagenes.py: no editar a mano.\n"
    "export const IMAGENES_SITIO: Record<string, { ancho: number; alto: number; variantes: number[] }> = "
    + json.dumps(medidas, indent=2) + "\n"
)
print("escrito", salida.relative_to(RAIZ.parent))
