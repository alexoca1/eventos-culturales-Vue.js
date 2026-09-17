# Quickstart — backend Eventos Culturales Puertollano

## Requisitos

- JDK 21, MySQL 8 (local o Aiven). Sin `mvn` instalado: usa el wrapper (`mvnw.cmd` en Windows).

## 1. Variables de entorno

```bash
# Desarrollo local (los defaults de application.properties bastan para arrancar)
JWT_SECRET=$(openssl rand -base64 32)   # obligatoria fuera de local; en local hay default de desarrollo
ADMIN_SEED_PASSWORD=CambiaEstaPass123    # password del admin sembrado (default: admin123)

# Opcional en local; obligatorio en prod (Aiven/Render)
DB_URL=jdbc:mysql://localhost:3306/eventos_culturales?useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=
PORT=8081
COOKIE_SECURE=false                      # true en producción (HTTPS)
COOKIE_SAME_SITE=Lax                     # None en producción (frontend y backend en distinto origen)
CORS_ALLOWED_ORIGINS=http://localhost:*  # en producción: https://tu-frontend.onrender.com
```

Crea la BD una vez: `CREATE DATABASE eventos_culturales;` (las tablas las crea Hibernate con `ddl-auto=update`).

## 2. Arrancar

```bash
cd backend
./mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # Linux/Mac
```

Al arrancar con la BD vacía se siembran `admin@test.com` + 5 eventos de ejemplo.

- API: `http://localhost:8081` · Swagger: `http://localhost:8081/swagger-ui.html`
- Login: `POST /auth/login {"email":"admin@test.com","password":"<ADMIN_SEED_PASSWORD>"}`

## 3. Tests

```bash
./mvnw.cmd test   # 38 tests: Mockito (servicios) + MockMvc/Security Test (autorización)
```

## 4. Docker (Render)

```bash
docker build -t eventos-backend ./backend
docker run -p 8081:8081 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e JWT_SECRET=... -e ADMIN_SEED_PASSWORD=... -e COOKIE_SECURE=true \
  -e CORS_ALLOWED_ORIGINS=https://tu-frontend.onrender.com eventos-backend
```

En Render: servicio Web con este repo, `Dockerfile` en `backend/`, y esas mismas env vars
(`COOKIE_SAME_SITE=None`, `CORS_ALLOWED_ORIGINS=https://eventosculturales.netlify.app`). Contrato de endpoints en `docs/api-contract.md`.

## 5. Frontend

Estático (sin build): se despliega tal cual en Netlify. En `frontend/js/eventos.js`, `RENDER_API` debe apuntar a la URL del backend en Render; en local/`file://` usa `http://localhost:8081` solo.
