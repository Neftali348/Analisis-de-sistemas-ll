# Guía del frontend en español

Esta versión renombra los archivos, componentes, servicios, interfaces y carpetas propias de Angular para que el proyecto sea más fácil de estudiar.

## Carpetas
- `core` → `nucleo`
- `layout` → `diseno`
- `pages` → `paginas`
- `internal` → `internas`
- `public` → `publicas`

## Ejemplos de clases
- `ApiService` → `ServicioApi`
- `AuthService` → `ServicioAutenticacion`
- `UsersComponent` → `ComponenteUsuarios`
- `CasesComponent` → `ComponenteCasos`
- `DashboardComponent` → `ComponentePanel`
- `Branch` → `Sucursal`
- `CaseView` → `VistaCaso`

## Por qué todavía verás algunas palabras en inglés
Se mantienen los nombres que pertenecen a Angular, TypeScript, HTTP o al contrato JSON con el backend: `HttpClient`, `Observable`, `subscribe`, `next`, `error`, `GET/POST`, y propiedades como `username`, `fullName`, `branchId`, etc. Mantener esas propiedades evita romper la API y la base de datos ya creadas.
