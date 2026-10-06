# Weather artwork

Ready-made static artwork from [Makin-Things/weather-icons](https://github.com/Makin-Things/weather-icons), pinned to commit `3793c6a1bf5c52402bb09e5d062b72f9fb5c0410` (`static/*.svg`). Original artwork and colors are preserved.

The upstream repository distributes its changes under [MIT](LICENSE). Its icons are based on [amCharts weather icons](https://www.amcharts.com/free-animated-svg-weather-icons/), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Credit: amCharts / Makin-Things. The app's forecast footer includes attribution; notices are also bundled in `app/src/main/assets/licenses/weather-icons.txt`.

The SVG files in this directory are the downloaded sources. `render.py` converts them to lossless 256 × 256 WebP resources with transparent margins, preserving proportions. No SVG renderer or image download is needed at runtime. To regenerate from the repository root using Python with CairoSVG 2.9.1 and Pillow 12.3.0 installed:

```sh
python3 third_party/weather-icons/render.py
```
