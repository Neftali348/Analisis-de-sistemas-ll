# Cómo usar este frontend

1. Sustituye tu carpeta `frontend` por esta versión o copia únicamente `src/app` sobre tu proyecto actual.
2. Si vienes de una versión anterior, elimina la caché de Angular:
   ```powershell
   Remove-Item -Recurse -Force .angular -ErrorAction SilentlyContinue
   ```
3. Instala dependencias si hace falta:
   ```powershell
   npm install
   ```
4. Inicia Angular:
   ```powershell
   npm start
   ```
5. Mantén Spring Boot ejecutándose en `http://localhost:8081`.

La organización de cada pantalla es:

```text
nombre-componente/
├── nombre-componente.component.ts
├── nombre-componente.component.html
├── nombre-componente.component.css
└── nombre-componente.service.ts
```

Los servicios compartidos permanecen en `src/app/nucleo`.
