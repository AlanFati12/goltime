# ADR 0006: Catálogo de sedes y canchas, horarios y cálculo de precios

- Estado: aceptada (Fase 2)
- Decisiones:
  1. **Horas como minutos del día.** Horarios y franjas se guardan como enteros (0..1440) y viajan en la API como "HH:mm" (hasta "24:00"). Evita la ambigüedad de un `TIME` para cierres a medianoche y simplifica la aritmética de intervalos.
  2. **Modalidad derivada.** El admin elige los jugadores por equipo y la modalidad ("F5") se deriva. Las modalidades válidas vienen de `configuracion.modalidades_permitidas`.
  3. **Canchas dependientes de un solo nivel.** Una cancha compuesta (F8) se forma con componentes (dos F5) de la misma sede; una componente no puede ser compuesta, ni una compuesta ser componente. Así la disponibilidad y la tabla `ocupaciones_cancha` (ADR 0003) se resuelven sin recursión.
  4. **Precio proporcional por tramos.** El cálculo vive en una clase de dominio pura (`CalculadoraPrecio`) y se reutiliza en la cotización pública y, desde la Fase 3, en la reserva para guardar la instantánea del precio. La franja de festivo prevalece sobre la del día.
  5. **Horario efectivo.** El de la cancha reemplaza al de la sede solo en los días que define; un día sin horario se considera cerrado.
  6. **Cambio de estado con conflictos.** Desactivar una sede o sacar una cancha de servicio exige motivo, previsualiza las reservas futuras afectadas y, si hay, exige confirmación explícita. Los puertos `ReservasDeEspacio` (y `ReservasDeUsuario`, de la Fase 1) tienen hoy un adaptador provisional que devuelve 0; la Fase 3 lo reemplaza.
  7. **Fotos.** El tipo se verifica por la firma de los primeros bytes (JPG, PNG, WebP), nunca por la extensión ni el Content-Type declarado; máximo 5 MB y 10 fotos; nombre aleatorio (UUID); el nombre se valida con una expresión estricta al leer o borrar para impedir salir del directorio. Se guardan en un volumen de Docker detrás del puerto `AlmacenArchivos` (reemplazable por S3).
  8. **Catálogo público bajo `/api/v1/publico/**`** (sedes, canchas, cotización, festivos y archivos). Sedes inactivas y canchas que no están `ACTIVA` no aparecen y responden 404.
  9. **Borrado lógico.** Sedes y canchas no se eliminan: se desactivan. Las tarifas y festivos sí se pueden borrar porque no tienen historial propio (la reserva guarda su precio como instantánea).
