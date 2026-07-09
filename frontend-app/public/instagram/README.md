# Fotos y videos de la galería "Nuestros trabajos" (landing)

Esta carpeta se sirve tal cual en `/instagram/...` — lo que pongas acá con
estos nombres exactos aparece automáticamente en la landing, sin tocar código.

## Archivos actuales

| Archivo       | Tipo  | Layout |
|----------------|-------|--------|
| `reel-1.mp4`   | video | tall   |
| `reel-2.mp4`   | video | tall   |
| `reel-3.mp4`   | video | tall   |

Son Reels bajados con `yt-dlp` (formato vertical), por eso los tres usan
`layout: 'tall'` — así cada uno ocupa una columna completa (3 columnas ×
2 filas) en vez de recortarse cuadrado. Podés mezclar fotos y videos
libremente; el `layout` (`tall`/`wide`/sin especificar) solo cambia cuánto
espacio ocupa cada ítem en la grilla.

## Recomendaciones

- **Fotos**: `.jpg` o `.webp`, apaisadas o cuadradas, ideal ~1200px de ancho.
  No hace falta que sean gigantes — se recortan a `object-fit: cover`.
- **Videos**: `.mp4` (H.264), livianos (idealmente **menos de 5-8 MB** cada uno)
  — se reproducen en loop automático y sin sonido en la landing (como un
  Reel), así que no importa el audio. Si el original pesa mucho, comprimilo
  antes (HandBrake, o simplemente descargalo con "calidad normal" desde
  Instagram en vez de la original). **Importante**: estos archivos se suben
  al repo como cualquier otro (`git add` + commit) porque el deploy del
  server es `git pull` — si son muy pesados, van a inflar el clone del repo,
  por eso conviene mantenerlos comprimidos.

## Para agregar/sacar/reordenar ítems

Los nombres y textos (caption, alt, si es foto o video, si ocupa 1 o 2
casilleros) se definen en:
`frontend-app/src/app/pages/landing-page/landing-page.ts` → array `GALERIA_INSTAGRAM`.

Cambiá el array (agregá o quitá objetos, o cambiá el `archivo`) y listo — el
HTML ya itera sobre esa lista, no hay que tocar el template.
