# Fotos y videos de la galería "Nuestros trabajos" (landing)

Esta carpeta se sirve tal cual en `/instagram/...` — lo que pongas acá con
estos nombres exactos aparece automáticamente en la landing, sin tocar código.

## Archivos esperados

| Archivo               | Tipo  | Dónde aparece en la grilla   |
|------------------------|-------|-------------------------------|
| `trabajo-1.jpg`        | foto  | columna izquierda, alta       |
| `trabajo-2.jpg`        | foto  | arriba centro                 |
| `proceso-armado.mp4`   | video | arriba derecha                |
| `trabajo-3.jpg`        | foto  | fila de abajo, ancha          |
| `empaquetado.mp4`      | video | abajo derecha                 |

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
