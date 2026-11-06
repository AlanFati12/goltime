#!/usr/bin/env python3
"""Carga datos de demostración realistas para INNOVASOFT IX (ver docs/guion-demo.md).

Qué crea (todo por la API real salvo el historial, que necesita fechas pasadas):
  - 2 sedes en Neiva (una con 3 canchas de F5 y 1 de F8 techada que depende de dos F5; otra con 2 de F7) con tarifas por franja.
  - 16 clientes con nombres colombianos (sin contraseña: en local entran con el ingreso de prueba de Google).
  - ~3 meses de reservas históricas con pagos, cancelaciones y no asistencias (para estadísticas y mapa de calor), y premios ganados.
  - Reservas futuras (pendientes y confirmadas), una serie recurrente, un bloqueo puntual y uno recurrente.
  - 2 promociones, 2 reglas de premio.
  - Torneos: uno finalizado (eliminación con descansos), uno en curso (grupos + eliminación) y uno con inscripciones abiertas.

Uso (desde la raíz del repositorio, con el sistema arriba y la base de datos vacía):
    docker compose --profile demo run --rm demo
"""
import datetime as dt
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request

import psycopg

API = os.environ.get("API_URL", "http://api:8080/api/v1")
ADMIN_CORREO = os.environ["ADMIN_CORREO"]
BOGOTA = dt.timezone(dt.timedelta(hours=-5))
random.seed(2026)


# ------------------------------------------------------------------ API
def llamar(metodo, ruta, token=None, cuerpo=None, esperado=(200, 201, 204)):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    cabeceras = {"Content-Type": "application/json"}
    if token:
        cabeceras["Authorization"] = "Bearer " + token
    for intento in range(3):
        peticion = urllib.request.Request(API + ruta, method=metodo, data=datos, headers=cabeceras)
        try:
            with urllib.request.urlopen(peticion) as r:
                texto = r.read()
                return r.status, (json.loads(texto) if texto else None)
        except urllib.error.HTTPError as e:
            texto = e.read()
            cuerpo_error = json.loads(texto) if texto else None
            if e.code == 429 and intento < 2:
                time.sleep(62)  # límite de intentos de ingreso por minuto
                continue
            if e.code in esperado:
                return e.code, cuerpo_error
            raise RuntimeError(f"{metodo} {ruta} -> {e.code}: {cuerpo_error}") from None
    raise RuntimeError("sin respuesta")


def ok(metodo, ruta, token=None, cuerpo=None):
    return llamar(metodo, ruta, token, cuerpo)[1]


def ingresar(correo):
    """Entra con el ingreso de MENTIRA de Google (GOOGLE_MODO_PRUEBA=true, solo desarrollo): no hay contraseñas."""
    return ok("POST", "/autenticacion/google", None, {"credencial": f"prueba|{correo}|Demo|Goltime", "recordarme": False})["tokenAcceso"]


def iso(fecha, hora, minuto=0):
    """Fecha local de Bogotá + hora -> instante ISO en UTC."""
    local = dt.datetime.combine(fecha, dt.time(hora, minuto), tzinfo=BOGOTA)
    return local.astimezone(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def dia_habil(base):
    while base.weekday() == 6:
        base += dt.timedelta(days=1)
    return base


# ------------------------------------------------------------------ Datos
NOMBRES = [
    ("Santiago", "Rojas Cuéllar"), ("Camila", "Ortiz Perdomo"), ("Juan David", "Trujillo Silva"), ("Valentina", "Cabrera Muñoz"),
    ("Andrés Felipe", "Quintero Vargas"), ("Daniela", "Méndez Polanía"), ("Sebastián", "Ramírez Tovar"), ("Laura Sofía", "Vanegas Ardila"),
    ("Carlos Mario", "Losada Gutiérrez"), ("María Fernanda", "Bahamón Cortés"), ("Nicolás", "Charry Peña"), ("Paula Andrea", "Medina Cabrera"),
    ("Julián", "Alvarado Lara"), ("Karen", "Sánchez Trujillo"), ("Brayan", "Cuenca Osorio"), ("Ángela", "Perdomo Ruiz"),
]
EQUIPOS = ["Los Cóndores del Huila", "Atlético Timanco", "Real Surcolombiana", "Deportivo Magdalena", "Halcones del Sur", "Tigres de la Gaitana",
           "Leones del Neiva", "Rayo Opita", "Club Ceibas FC", "Furia Rivera", "Bambuqueros FC", "Guacharacos United"]
APELLIDOS = ["Cuéllar", "Perdomo", "Silva", "Muñoz", "Vargas", "Polanía", "Tovar", "Ardila", "Gutiérrez", "Cortés", "Peña", "Lara", "Osorio", "Ruiz", "Rojas",
             "Medina", "Trujillo", "Sánchez", "Quintero", "Losada", "Charry", "Bahamón", "Cabrera", "Ramírez"]
PILA = ["Diego", "Jhon", "Yeison", "Kevin", "Camilo", "Esteban", "Mauricio", "Felipe", "Alexis", "Wilmer", "Edwin", "Cristian", "Johan", "Rafael", "Óscar", "Luis"]


def main():
    admin = ingresar(ADMIN_CORREO)
    if any(s["nombre"] == "Goltime Neiva Centro" for s in ok("GET", "/admin/sedes", admin)):
        print("Los datos de demostración ya están cargados. Para repetir: docker compose down -v y volver a arrancar.")
        return
    bd = psycopg.connect(host=os.environ.get("DB_HOST", "db"), port=int(os.environ.get("DB_PUERTO", "5432")), dbname=os.environ["DB_NOMBRE"],
                         user=os.environ["DB_USUARIO"], password=os.environ["DB_CLAVE"], autocommit=False)
    hoy = dt.datetime.now(BOGOTA).date()

    print("1/8 Sedes, canchas y tarifas…")
    centro = ok("POST", "/admin/sedes", admin, {"nombre": "Goltime Neiva Centro", "direccion": "Carrera 5 # 12-30", "ciudad": "Neiva", "telefono": "3101234567",
                                                  "whatsapp": "573101234567", "descripcion": "Canchas sintéticas de fútbol 5 y una cancha techada de fútbol 8 en el centro de Neiva.",
                                                  "servicios": ["Parqueadero", "Cafetería", "Baños", "Iluminación LED", "Vestieres"], "latitud": 2.9273, "longitud": -75.2819})
    norte = ok("POST", "/admin/sedes", admin, {"nombre": "Goltime Neiva Norte", "direccion": "Calle 26 # 8-50", "ciudad": "Neiva", "telefono": "3207654321",
                                                 "whatsapp": "573207654321", "descripcion": "Dos canchas de fútbol 7 con grama sintética de última generación.",
                                                 "servicios": ["Parqueadero", "Tienda deportiva", "Baños", "Iluminación LED"], "latitud": 2.9605, "longitud": -75.2900})

    def cancha(sede, nombre, jugadores, precio, techada=False, componentes=None):
        cuerpo = {"nombre": nombre, "jugadoresPorEquipo": jugadores, "precioBaseHora": precio, "techada": techada, "superficie": "Sintética profesional",
                  "iluminacion": True, "descripcion": f"{nombre}: grama sintética con iluminación LED."}
        if componentes:
            cuerpo["componentes"] = componentes
        return ok("POST", f"/admin/sedes/{sede['id']}/canchas", admin, cuerpo)

    f5 = [cancha(centro, f"Cancha {i} (F5)", 5, 70000) for i in (1, 2, 3)]
    f8 = cancha(centro, "Cancha Techada (F8)", 8, 130000, True, [f5[0]["id"], f5[1]["id"]])
    f7 = [cancha(norte, f"Cancha {i} (F7)", 7, 95000) for i in (1, 2)]
    canchas = [(c, 70000) for c in f5] + [(f8, 130000)] + [(c, 95000) for c in f7]
    for c, base in canchas:
        for dias, ini, fin, factor in (([1, 2, 3, 4, 5], "08:00", "12:00", 0.85), ([1, 2, 3, 4, 5], "18:00", "23:00", 1.2), ([6, 7], "08:00", "20:00", 1.25)):
            ok("POST", f"/admin/canchas/{c['id']}/tarifas", admin, {"diasSemana": dias, "horaInicio": ini, "horaFin": fin, "aplicaFestivo": False,
                                                                     "precioHora": int(base * factor / 1000) * 1000})
        ok("POST", f"/admin/canchas/{c['id']}/tarifas", admin, {"diasSemana": [], "horaInicio": "08:00", "horaFin": "23:00", "aplicaFestivo": True,
                                                                 "precioHora": int(base * 1.4 / 1000) * 1000})

    print("2/8 Clientes…")
    ids_clientes = []
    with bd.cursor() as cur:
        for i, (nombres, apellidos) in enumerate(NOMBRES):
            correo = f"{nombres.split()[0].lower()}.{apellidos.split()[0].lower()}@demo.goltime.test".replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            cur.execute("""INSERT INTO usuarios (nombres, apellidos, correo, telefono, documento, rol, correo_verificado, acepto_terminos_en, puede_inscribir_equipos)
                           VALUES (%s, %s, %s, %s, %s, 'CLIENTE', TRUE, now(), %s) RETURNING id""",
                        (nombres, apellidos, correo, f"+573{random.randint(100000000, 249999999)}", f"10{random.randint(10000000, 99999999)}", i in (1, 4)))
            ids_clientes.append((cur.fetchone()[0], correo))
        cur.execute("SELECT id FROM usuarios WHERE rol = 'ADMIN' ORDER BY id LIMIT 1")
        admin_id = cur.fetchone()[0]
        cur.execute("INSERT INTO clientes_mostrador (nombre, telefono) VALUES ('Equipo Los Amigos del Barrio', '+573115550101'), ('Don Hernando Perdomo', '+573125550202') RETURNING id")
        mostradores = [f[0] for f in cur.fetchall()]
    bd.commit()

    print("3/8 Historial de ~3 meses (reservas, pagos, cancelaciones)…")
    historial(bd, canchas, ids_clientes, mostradores, admin_id, hoy)

    print("4/8 Promociones y premios…")
    ok("POST", "/admin/promociones", admin, {"nombre": "Mañanas al 20%", "descripcion": "20% de descuento reservando de lunes a viernes antes del mediodía.", "tipo": "PORCENTAJE",
                                             "valor": 20, "fechaDesde": str(hoy), "fechaHasta": str(hoy + dt.timedelta(days=60)), "diasSemana": [1, 2, 3, 4, 5], "minutoInicio": 480,
                                             "minutoFin": 720, "publica": True})
    ok("POST", "/admin/promociones", admin, {"nombre": "Bienvenida INNOVASOFT", "descripcion": "10% de descuento con el código GOLTIME10.", "codigo": "GOLTIME10", "tipo": "PORCENTAJE",
                                             "valor": 10, "fechaDesde": str(hoy), "fechaHasta": str(hoy + dt.timedelta(days=90)), "usosPorCliente": 3, "publica": True})
    ok("POST", "/admin/premios/reglas", admin, {"nombre": "Hora con 15% de descuento", "reservasRequeridas": 5, "tipo": "PORCENTAJE", "valor": 15, "vigenciaDias": 60})
    ok("POST", "/admin/premios/reglas", admin, {"nombre": "Bono de $20.000", "reservasRequeridas": 10, "tipo": "MONTO_FIJO", "valor": 20000, "vigenciaDias": 90})
    premios_de_historial(bd)

    print("5/8 Reservas futuras, serie recurrente y bloqueos…")
    futuras(admin, f5, f8, f7, ids_clientes, mostradores, centro, norte, hoy)

    print("6/8 Equipos y jugadores…")
    equipos = crear_equipos(admin)

    print("7/8 Torneos…")
    torneo_finalizado(admin, norte, f7, equipos[:6])
    torneo_en_curso(admin, norte, f7, equipos[4:])
    torneo_abierto(admin, centro, equipos[:5], ids_clientes, hoy)

    bd.close()
    print("\nListo. Administrador:", ADMIN_CORREO, "· Clientes: ver docs/guion-demo.md (entran con Google; en local, con GOOGLE_MODO_PRUEBA=true)")


# ------------------------------------------------------------------ Historial
def historial(bd, canchas, clientes, mostradores, admin_id, hoy):
    """Reservas de los últimos 90 días directamente en la base (la API no admite fechas pasadas)."""
    ocupado = set()
    componentes = {canchas[3][0]["id"]: [canchas[0][0]["id"], canchas[1][0]["id"]]}
    horas_pesos = [(8, 1), (9, 1), (10, 1), (11, 1), (15, 1), (16, 2), (17, 3), (18, 5), (19, 6), (20, 6), (21, 4), (22, 2)]
    horas = [h for h, p in horas_pesos for _ in range(p)]
    metodos = ["EFECTIVO", "TRANSFERENCIA", "NEQUI", "DAVIPLATA", "TARJETA"]
    equipos_nombre = [None, "Los Cracks", "Amigos del Parque", "Oficina FC", "Los Veteranos", "Sub-23", "Compañeros USCO"]
    with bd.cursor() as cur:
        for atras in range(90, 0, -1):
            dia = hoy - dt.timedelta(days=atras)
            for cancha, base in canchas:
                cid = cancha["id"]
                cantidad = random.choice([1, 2, 3, 4, 4, 5, 6]) if dia.weekday() < 5 else random.choice([4, 5, 6, 7, 8])
                for hora in random.sample(sorted(set(horas)), k=min(cantidad, len(set(horas)))):
                    hora = random.choice(horas) if random.random() < 0.5 else hora
                    if dia.weekday() == 6 and hora >= 20:
                        continue
                    ocupa = [cid] + componentes.get(cid, [])
                    if any(((o, dia, hora) in ocupado) for o in ocupa):
                        continue
                    # una F5 ocupada también bloquea a la F8 que la contiene
                    if any(cid in comp and (f8, dia, hora) in ocupado for f8, comp in componentes.items()):
                        continue
                    ocupado.update((o, dia, hora) for o in ocupa)
                    factor = 0.85 if hora < 12 else 1.2 if hora >= 18 else 1.0
                    precio = int(base * factor / 1000) * 1000
                    azar = random.random()
                    estado = "COMPLETADA" if azar < 0.78 else "CANCELADA" if azar < 0.92 else "NO_ASISTIO"
                    inicio = dt.datetime.combine(dia, dt.time(hora), tzinfo=BOGOTA)
                    fin = inicio + dt.timedelta(hours=1)
                    admin_crea = random.random() < 0.25
                    if random.random() < 0.18:
                        cliente_id, mostrador_id = None, random.choice(mostradores)
                        admin_crea = True
                    else:
                        cliente_id, mostrador_id = random.choice(clientes)[0], None
                    pagado, estado_pago = 0, "PENDIENTE"
                    if estado == "COMPLETADA":
                        r = random.random()
                        pagado, estado_pago = (precio, "PAGADO") if r < 0.85 else (precio // 2, "ABONADO") if r < 0.93 else (0, "PENDIENTE")
                    cancelada = estado == "CANCELADA"
                    por_admin = cancelada and (cliente_id is None or random.random() < 0.4)
                    if cancelada and random.random() < 0.3:
                        pagado, estado_pago = precio, "REEMBOLSO_PENDIENTE"
                    cur.execute("""INSERT INTO reservas (codigo, cliente_id, cliente_mostrador_id, cancha_id, inicio, fin, estado, origen, precio_base, precio_final, estado_pago, monto_pagado,
                                       nombre_equipo, cancelada_por, rol_cancelador, motivo_cancelacion, fecha_cancelacion, cancelacion_tardia, aprobada_por, fecha_aprobacion, creado_en)
                                   VALUES ('GT-2026-' || lpad(nextval('reservas_codigo_seq')::text, 6, '0'), %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                                   RETURNING id""",
                                (cliente_id, mostrador_id, cid, inicio, fin, estado, "ADMIN" if admin_crea else "WEB", precio, precio, estado_pago, pagado,
                                 random.choice(equipos_nombre), (admin_id if por_admin else cliente_id) if cancelada else None,
                                 ("ADMIN" if por_admin else "CLIENTE") if cancelada else None,
                                 ("Cancha en mantenimiento por lluvia" if por_admin else "No puedo asistir") if cancelada else None,
                                 inicio - dt.timedelta(hours=random.choice([1, 3, 24, 30])) if cancelada else None, cancelada and random.random() < 0.3, admin_id,
                                 inicio - dt.timedelta(days=2), inicio - dt.timedelta(days=random.randint(2, 6))))
                    rid = cur.fetchone()[0]
                    if not cancelada:
                        for o in ocupa:
                            cur.execute("INSERT INTO ocupaciones_cancha (cancha_id, rango, tipo, reserva_id) VALUES (%s, tstzrange(%s, %s, '[)'), 'RESERVA', %s)", (o, inicio, fin, rid))
                    if pagado > 0:
                        tipo = "PAGO" if estado_pago in ("PAGADO", "REEMBOLSO_PENDIENTE") else "ABONO"
                        cur.execute("INSERT INTO pagos (reserva_id, monto, metodo, tipo, registrado_por, fecha) VALUES (%s, %s, %s, %s, %s, %s)",
                                    (rid, pagado, random.choice(metodos), tipo, admin_id, inicio - dt.timedelta(hours=random.choice([2, 24, 48]))))
    bd.commit()


def premios_de_historial(bd):
    """Premios coherentes con las reservas completadas de cada cliente: los viejos usados o vencidos, el último disponible."""
    with bd.cursor() as cur:
        cur.execute("SELECT id, reservas_requeridas, nombre, tipo, valor, vigencia_dias FROM reglas_premio ORDER BY id")
        reglas = cur.fetchall()
        cur.execute("SELECT cliente_id, count(*) FROM reservas WHERE estado = 'COMPLETADA' AND cliente_id IS NOT NULL GROUP BY cliente_id")
        for cliente_id, completadas in cur.fetchall():
            for regla_id, requeridas, nombre, tipo, valor, vigencia in reglas:
                ganados = completadas // requeridas
                for n in range(ganados):
                    ultimo = n == ganados - 1
                    estado = "DISPONIBLE" if ultimo and random.random() < 0.85 else random.choice(["USADO", "USADO", "VENCIDO"])
                    otorgado = "now() - interval '%d days'" % ((ganados - n) * 12 if not ultimo else random.randint(2, 20))
                    vence = "%s + interval '%d days'" % (otorgado, vigencia) if estado != "VENCIDO" else "now() - interval '5 days'"
                    cur.execute(f"INSERT INTO premios_cliente (cliente_id, regla_id, nombre, tipo, valor, estado, otorgado_en, vence_en) VALUES (%s, %s, %s, %s, %s, %s, {otorgado}, {vence})",
                                (cliente_id, regla_id, nombre, tipo, valor, estado))
    bd.commit()


# ------------------------------------------------------------------ Futuras
def futuras(admin, f5, f8, f7, clientes, mostradores, centro, norte, hoy):
    todas = [c["id"] for c in f5 + [f8] + f7]
    tokens = {}
    for cid, correo in clientes[:7]:
        tokens[cid] = ingresar(correo)
    reservas = []
    for i, (cid, token) in enumerate(tokens.items()):
        for _ in range(2):
            for intento in range(8):
                dia = dia_habil(hoy + dt.timedelta(days=random.randint(1, 10)))
                hora = random.choice([17, 18, 19, 20, 21])
                estado, cuerpo = llamar("POST", "/reservas", token, {"canchaId": random.choice(todas), "inicio": iso(dia, hora), "fin": iso(dia, hora + 1),
                                                                    "nombreEquipo": random.choice(["Los Cracks", "Oficina FC", "Amigos del Parque"])}, esperado=(409, 422))
                if estado == 201:
                    reservas.append(cuerpo["id"])
                    break
    for n, rid in enumerate(reservas):
        if n % 5 != 4:  # unas pocas quedan pendientes para mostrar la aprobación
            ok("POST", f"/admin/reservas/{rid}/aprobar", admin, {})
    # reservas del administrador para clientes de mostrador
    dia = dia_habil(hoy + dt.timedelta(days=3))
    llamar("POST", "/admin/reservas", admin, {"canchaId": f5[2]["id"], "inicio": iso(dia, 20), "fin": iso(dia, 21), "mostrador": {"nombre": "Don Hernando Perdomo", "telefono": "3125550202"},
                                              "nombreEquipo": "Los Veteranos"}, esperado=(400, 409, 422))
    # serie recurrente: los miércoles a las 20:00 durante 8 semanas
    miercoles = hoy + dt.timedelta(days=(2 - hoy.weekday()) % 7 + 7)
    llamar("POST", "/admin/series", admin, {"canchaId": f7[0]["id"], "inicio": iso(miercoles, 20), "fin": iso(miercoles, 21), "semanas": 8, "clienteId": clientes[0][0],
                                            "nombreEquipo": "Los Miércoles", "omitirOcupadas": True}, esperado=(400, 409, 422))
    # bloqueos
    jueves = dia_habil(hoy + dt.timedelta(days=(3 - hoy.weekday()) % 7 + 7))
    llamar("POST", "/admin/bloqueos", admin, {"sedeId": centro["id"], "canchaIds": [f5[2]["id"]], "tipo": "PUNTUAL", "inicio": iso(jueves, 8), "fin": iso(jueves, 12), "motivo": "MANTENIMIENTO",
                                              "detalle": "Cambio de grama en la cancha 3"}, esperado=(400, 409, 422))
    llamar("POST", "/admin/bloqueos", admin, {"sedeId": norte["id"], "tipo": "RECURRENTE", "diasSemana": [1], "horaInicio": "08:00", "horaFin": "10:00", "fechaDesde": str(hoy),
                                              "fechaHasta": str(hoy + dt.timedelta(days=90)), "motivo": "MANTENIMIENTO", "detalle": "Limpieza y revisión semanal de las canchas"}, esperado=(400, 409, 422))


# ------------------------------------------------------------------ Equipos y torneos
def crear_equipos(admin):
    equipos = []
    usados = set()
    for nombre in EQUIPOS:
        e = ok("POST", "/admin/equipos", admin, {"nombre": nombre, "color": "#%06x" % random.randint(0x2255aa, 0xddaa44), "delegadoNombre": f"{random.choice(PILA)} {random.choice(APELLIDOS)}",
                                                  "delegadoTelefono": f"3{random.randint(100000000, 249999999)}"})
        eid = e["equipo"]["id"]
        for dorsal in range(1, 9):
            while True:
                nombre_j, ap = random.choice(PILA), f"{random.choice(APELLIDOS)} {random.choice(APELLIDOS)}"
                if (nombre_j, ap) not in usados:
                    usados.add((nombre_j, ap))
                    break
            ok("POST", f"/admin/equipos/{eid}/jugadores", admin, {"nombres": nombre_j, "apellidos": ap, "documento": f"11{random.randint(10000000, 99999999)}{dorsal}", "dorsal": dorsal,
                                                                    "posicion": random.choice(["Portero", "Defensa", "Volante", "Delantero"])})
        equipos.append(eid)
    return equipos


def crear_torneo(admin, sede, canchas, cuerpo, equipos):
    base = {"sedeId": sede["id"], "jugadoresPorEquipo": 7, "modoEmparejamiento": "SEMBRADO", "canchaIds": [c["id"] for c in canchas], "valorInscripcion": 150000,
            "premiosTexto": "Trofeo y $1.000.000 para el campeón; $400.000 para el subcampeón.", "reglamento": "Partidos de 2 tiempos de 20 minutos. Se aplica el reglamento FIFA de fútbol 7."}
    base.update(cuerpo)
    torneo = ok("POST", "/admin/torneos", admin, base)["torneo"]["id"]
    for i, e in enumerate(equipos, 1):
        ok("POST", f"/admin/torneos/{torneo}/inscripciones", admin, {"equipoId": e, "semilla": i})
    return torneo


def partidos(admin, torneo):
    return ok("GET", f"/admin/torneos/{torneo}/partidos", admin)


def jugar(admin, torneo, partido, ganador_local=None, empate_penales=False):
    """Registra un resultado con goles, asistencias y tarjetas coherentes con el marcador."""
    gl, gv = random.choice([(1, 0), (2, 1), (3, 1), (2, 0), (1, 1), (0, 2), (4, 2), (3, 2)])
    if ganador_local is True and gl <= gv:
        gl, gv = gv + 1, gl
    if ganador_local is False and gv <= gl:
        gl, gv = gv, gl + 1
    penales = (None, None)
    if partido["tipoFase"] == "ELIMINATORIA" and gl == gv:
        gl += 1
    plantillas = {}
    for lado in ("local", "visitante"):
        plantillas[lado] = ok("GET", f"/admin/torneos/{torneo}/inscripciones/{partido[lado]['inscripcionId']}/jugadores", admin)
    pid = partido["id"]
    for lado, goles in (("local", gl), ("visitante", gv)):
        for _ in range(goles):
            autor = random.choice(plantillas[lado])
            minuto = random.randint(1, 40)
            detalle = ok("POST", f"/admin/partidos/{pid}/eventos", admin, {"inscripcionId": partido[lado]["inscripcionId"], "jugadorId": autor["id"], "tipo": "GOL", "minuto": minuto})
            if random.random() < 0.6:
                gol = [e for e in detalle["eventos"] if e["tipo"] == "GOL" and e["jugadorId"] == autor["id"] and e["minuto"] == minuto][-1]
                otro = random.choice([j for j in plantillas[lado] if j["id"] != autor["id"]])
                llamar("POST", f"/admin/partidos/{pid}/eventos", admin, {"inscripcionId": partido[lado]["inscripcionId"], "jugadorId": otro["id"], "tipo": "ASISTENCIA", "minuto": minuto,
                                                                          "eventoRelacionadoId": gol["id"]}, esperado=(400,))
        for _ in range(random.choice([0, 1, 1, 2])):
            ok("POST", f"/admin/partidos/{pid}/eventos", admin, {"inscripcionId": partido[lado]["inscripcionId"], "jugadorId": random.choice(plantillas[lado])["id"],
                                                                  "tipo": random.choice(["AMARILLA", "AMARILLA", "AMARILLA", "ROJA"]), "minuto": random.randint(5, 40)})
    ok("PUT", f"/admin/partidos/{pid}/resultado", admin, {"golesLocal": gl, "golesVisitante": gv, "penalesLocal": penales[0], "penalesVisitante": penales[1],
                                                           "figuraJugadorId": random.choice(plantillas["local" if gl >= gv else "visitante"])["id"]})


def torneo_finalizado(admin, sede, canchas, equipos):
    t = crear_torneo(admin, sede, canchas, {"nombre": "Copa Universitaria USCO 2026", "formato": "ELIMINACION_DIRECTA", "tercerPuesto": True, "cupoEquipos": 8,
                                            "descripcion": "Torneo de eliminación directa entre equipos de la comunidad universitaria.", "fechaInicio": str(dt.date.today() - dt.timedelta(days=40)),
                                            "fechaFin": str(dt.date.today() - dt.timedelta(days=30))}, equipos)
    ok("POST", f"/admin/torneos/{t}/fixture", admin, {})
    ok("POST", f"/admin/torneos/{t}/estado", admin, {"estado": "EN_CURSO"})
    while True:
        pendientes = [p for p in partidos(admin, t) if p["estado"] in ("PENDIENTE", "PROGRAMADO") and not p.get("descanso") and p.get("local") and p.get("visitante")
                      and p["local"].get("inscripcionId") and p["visitante"].get("inscripcionId")]
        if not pendientes:
            break
        jugar(admin, t, pendientes[0])
    ok("POST", f"/admin/torneos/{t}/estado", admin, {"estado": "FINALIZADO"})


def torneo_en_curso(admin, sede, canchas, equipos):
    t = crear_torneo(admin, sede, canchas, {"nombre": "Liga Relámpago Neiva", "formato": "GRUPOS_Y_ELIMINACION", "numGrupos": 2, "clasificadosPorGrupo": 2, "tercerPuesto": True,
                                            "cupoEquipos": 8, "descripcion": "Fase de grupos y eliminatoria: los dos mejores de cada grupo pasan a semifinales.",
                                            "fechaInicio": str(dt.date.today() - dt.timedelta(days=12)), "fechaFin": str(dt.date.today() + dt.timedelta(days=14))}, equipos)
    ok("POST", f"/admin/torneos/{t}/fixture", admin, {})
    ok("POST", f"/admin/torneos/{t}/estado", admin, {"estado": "EN_CURSO"})
    for p in partidos(admin, t):
        if p["tipoFase"] == "GRUPOS":
            jugar(admin, t, p)
    semis = [p for p in partidos(admin, t) if p["nombreRonda"] == "Semifinal"]
    if semis:
        jugar(admin, t, semis[0], ganador_local=True)
    # los partidos que faltan quedan programados en la sede, con reserva de cancha
    dia = dia_habil(dt.date.today() + dt.timedelta(days=2))
    hora = 19
    for p in partidos(admin, t):
        if p["estado"] == "PENDIENTE" and p.get("local") and p["local"].get("inscripcionId") and p.get("visitante") and p["visitante"].get("inscripcionId"):
            llamar("PUT", f"/admin/partidos/{p['id']}/programacion", admin, {"canchaId": canchas[0]["id"], "inicio": iso(dia, hora), "duracionMin": 60, "arbitro": "Carlos Gómez"},
                   esperado=(409, 422, 400))
            dia = dia_habil(dia + dt.timedelta(days=1))


def torneo_abierto(admin, sede, equipos, clientes, hoy):
    t = ok("POST", "/admin/torneos", admin, {"nombre": "Copa Verano Goltime", "sedeId": sede["id"], "jugadoresPorEquipo": 5, "formato": "LIGA", "idaYVuelta": True, "cupoEquipos": 8,
                                             "modoEmparejamiento": "ALEATORIO", "valorInscripcion": 120000, "descripcion": "Liga de todos contra todos, ida y vuelta, en las canchas del centro.",
                                             "fechaInicio": str(hoy + dt.timedelta(days=20)), "fechaFin": str(hoy + dt.timedelta(days=60)), "premiosTexto": "Trofeo y camisetas para el campeón."})["torneo"]["id"]
    for e in equipos[:4]:
        ok("POST", f"/admin/torneos/{t}/inscripciones", admin, {"equipoId": e})
    ok("POST", f"/admin/torneos/{t}/estado", admin, {"estado": "INSCRIPCIONES"})
    # una solicitud pendiente hecha por un cliente habilitado, para mostrar la aprobación
    cid, correo = clientes[1]
    token = ingresar(correo)
    jugadores = [{"nombres": random.choice(PILA), "apellidos": f"{random.choice(APELLIDOS)} {random.choice(APELLIDOS)}", "documento": f"12{random.randint(10000000, 99999999)}{i}", "dorsal": i}
                 for i in range(1, 6)]
    llamar("POST", f"/torneos/{t}/solicitudes", token, {"nombreEquipo": "Los Nuevos del Barrio", "color": "#e11d48", "jugadores": jugadores}, esperado=(409, 422, 400))


if __name__ == "__main__":
    try:
        main()
    except Exception as ex:  # noqa: BLE001
        print("ERROR:", ex, file=sys.stderr)
        sys.exit(1)
