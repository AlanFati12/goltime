# Imagen del cargador de datos de demostración (solo se construye con el perfil "demo").
FROM python:3.12-slim
RUN pip install --no-cache-dir "psycopg[binary]" bcrypt
WORKDIR /demo
COPY cargar_demo.py .
CMD ["python", "cargar_demo.py"]
