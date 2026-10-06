"""Convert the vendored upstream SVGs to bundled Android WebP resources.

Requires cairosvg==2.9.1 and pillow==12.3.0. Run from the repository root.
Only rasterization, transparent-margin trimming and proportional resizing occur.
"""
from pathlib import Path
import io

import cairosvg
from PIL import Image

source = Path(__file__).resolve().parent
target = source.parents[1] / 'app/src/main/res/drawable-nodpi'
for svg in sorted(source.glob('*.svg')):
    image = Image.open(io.BytesIO(cairosvg.svg2png(url=str(svg), output_width=512))).convert('RGBA')
    image = image.crop(image.getbbox())
    image.thumbnail((224, 224), Image.Resampling.LANCZOS)
    canvas = Image.new('RGBA', (256, 256))
    canvas.alpha_composite(image, ((256 - image.width) // 2, (256 - image.height) // 2))
    canvas.save(target / ('weather_' + svg.stem.replace('-', '_') + '.webp'), lossless=True)
