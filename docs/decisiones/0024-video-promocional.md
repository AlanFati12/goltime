# ADR 0024: Video promocional hecho con código

- Estado: aceptada
- Contexto: INNOVASOFT IX pide un video de 60 s («Proyecto de software finalizado») que muestre el software funcionando y un resultado verificable. Un editor de video manual no deja rastro reproducible ni sincroniza cada golpe visual con la voz.
- Decisión:
  1. **Proyecto aparte** (carpeta `video`), fuera de este repositorio y de `frontend/` y `backend/`, con su propio `package.json`: no afecta la compilación ni los contenedores.
  2. **Remotion (React) para la animación**: escenas, subtítulos y golpes visuales leen los tiempos reales de cada palabra (`src/datos/tiempos.json`), y el mapa de planos (`src/datos/escenas.json`) lo comparten la animación y la mezcla de audio.
  3. **App real grabada, no maquetas**:
     - Playwright sobre una copia aislada de Goltime (proyecto compose `goltime-video`, volúmenes propios, datos de demostración y administrador `admin@demo.goltime.test`).
     - Captura a resolución completa grabando 1,5 veces más lento y acelerando después, porque el video de Playwright es VP8 de baja tasa y el screencast de Chrome ignora la densidad de píxeles.
  4. **Voz con edge-tts** (`es-CO-SalomeNeural`) por frases, con tiempos por palabra. **Música y efectos sintetizados** con NumPy/SciPy: no hay material de terceros que licenciar.
  5. **Cifras solo verificadas** y guardadas en `src/datos/resultados.json`, con la salida de las pruebas que las respalda.
- Consecuencias: el video se rehace con `npm run voz`, `npm run grabar`, `npm run audio` y `npm run render`. Si la interfaz cambia, hay que volver a grabar y revisar las coordenadas de cámara y anillos de `escenas.json`. Los supuestos y créditos se guardan con el proyecto del video, fuera del repositorio.
