# Guía del código en español

Esta versión renombra **las clases, interfaces, enums, componentes y paquetes propios del proyecto** para que sean más fáciles de estudiar.

## Qué se tradujo

- `controller` → `controlador`
- `entity` → `entidad`
- `enums` → `enumeracion`
- `repository` → `repositorio`
- `security` → `seguridad`
- `service` → `servicio`
- `util` → `utilidad`
- Clases de negocio como `User`, `Branch`, `ComplaintCase`, `Role` y `Permission` ahora son `Usuario`, `Sucursal`, `Caso`, `Rol` y `Permiso`.
- Servicios y controladores también se nombraron en español.
- Los DTO ahora usan nombres como `SolicitudUsuario`, `VistaUsuario`, `SolicitudCierre` y `VistaCasoInterno`.

## Qué se dejó en inglés a propósito

Se conservaron las palabras que pertenecen al lenguaje o a los frameworks: `public`, `private`, `class`, `interface`, `record`, `String`, `Long`, `List`, `Map`, `@Entity`, `@Service`, `@RestController`, `JpaRepository`, `HttpServletRequest`, JWT, HTTP, etc. Cambiarlas no es posible o haría el código incorrecto.

También se conservaron **los nombres de campos JSON, columnas de base de datos y rutas de la API** para no romper la base ya creada ni la comunicación con Angular. Por ejemplo, internamente la clase ahora se llama `Usuario`, pero campos como `username`, `fullName` o rutas como `/api/auth/login` se mantienen por compatibilidad.

## Ejemplos

- `User` → `Usuario`
- `RoleCode` → `CodigoRol`
- `ComplaintCase` → `Caso`
- `CaseService` → `ServicioCasos`
- `CaseController` → `ControladorCasos`
- `AuditService` → `ServicioAuditoria`
- `JwtAuthenticationFilter` → `FiltroAutenticacionJwt`
- `DataInitializer` → `InicializadorDatos`
