# Correcciones de compilación

Se corrigieron los errores reportados por Maven el 13/08/2026.

Archivos corregidos:
- `configuracion/InicializadorDatos.java`
- `servicio/ServicioCodigoCaso.java`
- `servicio/ServicioReglasCaso.java`
- `servicio/ServicioEvidencias.java`
- `servicio/ServicioSeguridadArchivos.java`
- `servicio/ServicioReportes.java`
- `servicio/ServicioAdministracionRoles.java`
- `servicio/ServicioAdministracionUsuarios.java`

Cambios principales:
- Se reemplazaron expresiones `switch ->` por `switch` clásico para evitar los errores de análisis sintáctico observados.
- Se eliminó pattern matching en `instanceof` dentro de `ServicioEvidencias`.
- Se corrigieron escapes de expresiones regulares (`\\s+`) en `ServicioAdministracionUsuarios`.
- Se hizo más explícita la selección de formato PDF/XLSX en `ServicioReportes`.

Validación local realizada:
- `javac --release 17` no reportó errores sintácticos (`illegal start`, `orphaned case`, `illegal escape`, `; expected`, `) expected`).
- El entorno de generación no contiene Maven ni las dependencias Spring descargadas, por lo que el build completo debe comprobarse en el equipo del usuario.

En Windows, dentro de `backend`:

```powershell
java -version
mvn -version
mvn clean spring-boot:run
```

Se requiere Java 17 o superior compatible con Spring Boot 3.5.5.
