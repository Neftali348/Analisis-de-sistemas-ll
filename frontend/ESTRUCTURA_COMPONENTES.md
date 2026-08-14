# Estructura Angular por componente

Cada pantalla se organizó en su propia carpeta con cuatro archivos:

- `*.component.ts`: lógica de la pantalla.
- `*.component.html`: plantilla HTML.
- `*.component.css`: estilos propios de la pantalla.
- `*.service.ts`: llamadas al backend y acceso a datos de esa pantalla.

La carpeta `nucleo/` conserva servicios realmente compartidos, como autenticación, interceptor, guardias, modelos y el cliente HTTP base.

Ejemplo:

```text
paginas/internas/usuarios/
├── usuarios.component.ts
├── usuarios.component.html
├── usuarios.component.css
└── usuarios.service.ts
```

Las rutas de API se conservaron exactamente como las espera Spring Boot (`/api/public/...`, `/api/admin/...`, etc.). No se tradujeron los nombres del contrato JSON para no romper la integración con el backend.
