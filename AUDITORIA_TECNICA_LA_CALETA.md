> Actualización de demostración: la migración, 22 insumos y 24 recetas ya se aplicaron en la rama `demo-caleta` del proyecto `la-caleta-db`. Ambos checkouts se verificaron contra Neon y la autenticación SMTP pasó. Véase [Configuración y scripts de demostración](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/DEMOSTRACION.md>). El contenido siguiente conserva el estado de la auditoría original; sus pendientes de configuración fueron atendidos para esa rama.

# Auditoría técnica de La Caleta

Fecha: 16 de octubre de 2026. Proyecto revisado: `Caleta_Guillermo/lacaleta`.

**Resultado:** se corrigieron fallos de seguridad, integridad de compra, consultas y UI. La compilación y las pruebas locales pasan. La puesta en producción sigue condicionada a configurar las variables de entorno, aplicar el esquema en una rama de Neon, registrar las recetas reales y verificar SMTP. No se certifica una ejecución contra Neon ni una entrega real de correo con las pruebas locales.

Se conservaron los cambios locales de la integración que ya existían al iniciar la auditoría. No se hicieron commits ni se reemplazaron los avances fusionados.

## 1. Hallazgos y correcciones aplicadas

| Prioridad | Error exacto observado | Consecuencia | Corrección |
| --- | --- | --- | --- |
| Crítica | `AdminRestController.getDashboardMetrics()` no recibía sesión ni comprobaba permisos; los controladores `/admin/pizzas/**` y `/admin/insumos/**` tampoco tenían una barrera común. | Acceso directo a métricas y operaciones administrativas sin pasar por la vista protegida. | `AdminInterceptor` cubre páginas, reportes y API; exige un administrador verificado y vuelve a consultar su rol en BD. API devuelve 401/403. |
| Crítica | `payment.js` usaba `setTimeout(..., 1800)`, eliminaba el carrito y redirigía a `/pedido-confirmado` sin hacer una petición al backend. Esa ruta no tenía controlador. | Compra aparente sin pedido persistido y pérdida del carrito. | Formulario y botón final reales; petición a `/api/checkout`; carrito se elimina solo tras una respuesta satisfactoria. |
| Alta | `PedidoService` solo ejecutaba `producto.setStock(...)`; no existía relación entre productos e insumos. | Una venta nunca consumía materia prima. | Nueva entidad `RecetaInsumo`, receta configurable y consumo acumulado de insumos dentro de la misma transacción. Venta de alimentos sin receta devuelve conflicto. |
| Alta | `pedidos.saveAndFlush(pedido); email.enviarBoleta(...)` se ejecutaba antes del commit. | SMTP podía enviar una boleta de un pedido que después se revirtiera. Además mantenía bloqueos de inventario durante el envío. | Outbox persistente: pedido, existencias y HTML pendiente se confirman juntos; un trabajador procesa únicamente filas confirmadas y reintenta errores SMTP. |
| Alta | `pedidoRepository.findAll().stream().filter(...)` en `/perfil`. | Carga de todos los pedidos y sus usuarios; crecimiento del coste con toda la tienda y riesgo N+1. | Filtro por usuario y paginación en SQL, 20 pedidos por página, totales agregados en BD e índices propuestos. |
| Alta | Nombres, imágenes y detalles del carrito se interpolaban directamente en `innerHTML`, incluyendo contenido llegado desde parámetros de URL. | Posible inyección HTML y ejecución de atributos de eventos al renderizar el carrito. | Escape de HTML para nombres, imágenes y detalles en carrito, envío y pago; prueba de regresión con etiquetas inyectadas. |
| Alta | `alert()` en `perfil.html`, originalmente en cinco puntos: copia de cupón y validaciones de nombre, teléfono y DNI. | Incumplimiento de la regla de UI, incluso aunque los `.js` externos no tuvieran alertas. | Los cinco puntos usan Bootstrap Modal. También se sustituyeron `confirm()` de administración y notificaciones de carrito/flash. |
| Alta | `<input type="hidden" id="edit-stock" name="stock" value="10">`. | Editar nombre o precio podía restablecer silenciosamente las existencias a 10. | Campo visible, precargado con el stock real de la fila y validación de valores no negativos. |
| Alta | Eliminación de pizza con `@GetMapping("/eliminar/{id}")` y enlace GET. | Una navegación podía borrar datos. | POST mediante formulario y confirmación en modal. GET ahora devuelve 405. |
| Alta | Contraseñas de Neon/SMTP en `application.properties` y una contraseña de BD en `DbChecker.java`. | Exposición de credenciales en archivos y en el historial del repositorio. | Variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`; se regeneró el diagnóstico compilado sin credenciales literales. Las claves expuestas deben rotarse. |
| Media | La masa de ejemplo usa `unidades`, pero formularios y controlador de insumos solo aceptaban kg, g y litros. | Editar ese insumo fallaba o cambiaba su unidad al abrir el modal. | Se admite y conserva `unidades` en formulario, JavaScript y controlador; prueba de regresión del caso. |
| Media | La compra no guardaba `metodoPago` ni `direccionEntrega` y siempre sumaba S/ 5.00. | Estadísticas de pagos incompletas y cobro de envío incluso al retirar. | Datos validados por backend y persistidos; retiro cuesta cero, delivery S/ 5.00. |
| Media | Repetir el POST podía generar otra compra. | Pedidos y descuentos de inventario duplicados al perderse una respuesta de red. | Clave UUID por intento, conservada al reintentar; unicidad por usuario y bloqueo del usuario antes de comprobar existencia. |
| Media | Dashboard usaba varios `findAll()` y recorridos de entidades; reportes incluían etiquetas de pizzas/insumos inventadas como alternativa. | Consultas y memoria innecesarias, y datos visuales que no provenían del catálogo real. | Consultas `COUNT`, `SUM`, agrupaciones y top limitado en BD; listas vacías cuando no hay datos. Fechas calculadas en `America/Lima`. |
| Media | Eliminación del perfil en varias transacciones y canje de cupones sin serialización por cliente. | Posibles eliminaciones parciales y canjes simultáneos por encima del saldo. | Eliminación atómica; canje bloquea el usuario y recalcula consumo dentro de su transacción. Códigos de cupón UUID. |
| Media | Se serializaba `Usuario`, incluyendo el hash de contraseña, en el login. | Exposición innecesaria del hash al navegador/localStorage. | `@JsonIgnore` en la contraseña y renovación del ID de sesión tras autenticar. |
| Media | Archivos de imágenes conservaban extensiones entregadas por el cliente. | Posibilidad de servir archivos activos desde `/uploads`. | Solo firmas PNG/JPEG, tamaño limitado, nombre UUID y extensión asignada por el servidor; validación de las rutas/URL admitidas. |

## 2. Checkout e integridad transaccional

El método mantiene obligatoriamente:

```java
@Transactional(rollbackFor = Exception.class)
public Pedido procesarCompra(CheckoutRequest solicitud, Integer usuarioId)
```

Su secuencia es: bloquear usuario, comprobar cuenta verificada e idempotencia, validar entrada, agrupar productos duplicados, bloquear productos por ID ascendente, calcular precios desde BD, acumular ingredientes según recetas, bloquear insumos por ID ascendente, descontar ambos stocks, guardar pedido/detalles y encolar el HTML de la boleta.

Los bloqueos son `PESSIMISTIC_WRITE`; `@Version` protege también frente a escrituras concurrentes desde la gestión administrativa. El orden consistente de bloqueo reduce los interbloqueos. La semántica de bloqueo está documentada en [Spring Data JPA](https://www.springframework.org/spring-data/data-jpa/reference/jpa/locking.html).

Los importes enviados por el navegador no se aceptan. El servicio calcula con `BigDecimal` y redondea a dos decimales. Las entidades originales todavía almacenan algunos importes y existencias como `Double`: para exigencias contables o volúmenes mayores, queda pendiente migrar esos campos y el esquema a `numeric`/`BigDecimal` de extremo a extremo.

Las recetas usan cantidades decimales en la unidad del insumo. Si dos productos comparten un ingrediente, se suma su consumo antes de validarlo. No se asignaron cantidades ficticias. Las categorías `bebida`/`bebidas` pueden venderse por stock de producto sin receta; los demás productos requieren receta. La receta se configura desde el botón **Receta** del catálogo administrativo o mediante la API protegida `PUT /api/admin/productos/{id}/receta`.

Ejemplo de estructura de la API — los IDs y las cantidades deben corresponder a datos reales:

```json
[
  {"insumoId": 1, "cantidad": "0.250000"},
  {"insumoId": 2, "cantidad": "0.100000"}
]
```

## 3. Correo HTML y OTP

`EmailService` conserva la plantilla Thymeleaf `boleta-email.html`, codificación UTF-8, destinatario de la cuenta autenticada y `MimeMessageHelper.setText(html, true)`. El HTML se prepara durante la compra; si falla su generación, se revierte toda la transacción.

`BoletaOutboxService` guarda una copia del HTML y del destinatario en `boletas_pendientes`, dentro de la transacción de compra. El trabajador programado consulta lotes de hasta 10 filas cada 10 segundos con `FOR UPDATE SKIP LOCKED`, para permitir varios servidores. En caso de fallo, aumenta el intervalo de reintento hasta una hora. Un fallo SMTP ya no borra una compra confirmada. El frontend anuncia que la boleta está pendiente de envío.

El envío es de al menos una vez: si SMTP acepta el mensaje y el proceso cae antes de confirmar la eliminación de la fila, puede enviarse nuevamente. No se afirma entrega exactamente una vez ni llegada a la bandeja del destinatario. En explotación deberá vigilarse la antigüedad de las filas pendientes. La outbox evita depender de un evento solo en memoria; el concepto de ejecución después del commit también está descrito en [Spring Framework](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html).

El registro ya tenía una base correcta y se conservó: cuenta no verificada, rol cliente fijado por servidor, BCrypt, OTP aleatorio de seis dígitos con hash persistido, vencimiento de 10 minutos, cinco intentos y consumo de una sola vez. El login rechaza `verificado=false` y `null`. Se añadió comprobación de verificación en checkout y una espera de 60 segundos entre reenvíos de OTP. También se validaron los límites de longitud del nombre completo y correo respecto al esquema.

Quedan dos aspectos de autenticación ajenos al registro que impiden declarar un endurecimiento completo para producción: la recuperación de contraseña aún conserva su OTP en memoria de una instancia, y la autenticación mantiene una ruta de compatibilidad para contraseñas antiguas en texto plano. Deben migrarse las cuentas antiguas y persistir/limitar la recuperación antes de desplegar varias instancias. Las cuentas pendientes no pueden iniciar sesión; no se implementó una política de eliminación automática de registros pendientes abandonados.

## 4. Administración y consultas

Las páginas `/admin`, `/administracion`, `/reportes`, sus operaciones y `/api/admin/**` pasan por el interceptor del backend. Se verifica el rol actual de la cuenta en BD, por lo que una sesión antigua no conserva privilegios después de retirarlos. Los controles existentes de los controladores de páginas se conservaron.

Se añadió una comprobación de origen para mutaciones mediante `Origin` y `Sec-Fetch-Site`. Es una defensa para peticiones de navegador con cookies; no sustituye un sistema general de permisos ni un mecanismo CSRF por tokens. El logout existente continúa siendo GET.

Las métricas de ventas, clientes, catálogo, insumos críticos, métodos de pago y productos vendidos vienen de repositorios. Las agrupaciones se ejecutan en BD y los top se limitan antes de devolver los datos. La búsqueda de insumos también se filtra en BD. El seeder de ejemplo y el listado de diagnóstico solo se ejecutan con el perfil explícito `demo`.

Aún hay oportunidades de escala fuera del historial de cliente y las métricas: el listado administrativo de pedidos/clientes y algunas selecciones del catálogo cargan listas completas; la generación del código correlativo de insumo también recorre los códigos existentes. Esos flujos deberán paginarse o migrarse a una secuencia si crece el volumen. No se obtuvieron planes `EXPLAIN` ni mediciones de latencia sobre PostgreSQL real. Las sumas representan importes de pedidos registrados y los gráficos de pago reflejan el método declarado; no constituyen conciliación de cobros de una pasarela.

## 5. Perfil, N+1 y fragmentos

El perfil usa una consulta paginada restringida por `usuario_id`, orden estable por fecha e ID y total acumulado con `SUM`. Cada página contiene como máximo 20 pedidos. El repositorio carga explícitamente el usuario para evitar consultas implícitas por las relaciones eager.

La consulta del historial con detalles utiliza `@EntityGraph` para usuario, detalles, producto y categoría. Una prueba con Hibernate cuenta sentencias y confirma una sola consulta y cero consultas adicionales al recorrer las relaciones, incluso fuera del contexto transaccional del repositorio. La página paginada requiere como máximo dos sentencias para contenido y conteo. El perfil ejecuta además consultas separadas para total, último pedido y cupones. [Referencia de EntityGraph](https://docs.spring.io/spring-data/data-jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html).

Se conservaron `th:replace` y `th:fragment`. La prueba de referencias recorre todas las plantillas y verifica que cada fragmento explícito utilizado exista. Se renderizan plantillas reales de perfil, administración, registro, carrito, envío, pago y páginas públicas mediante MockMvc. Se añadió un fragmento reutilizable para transportar notificaciones flash a Bootstrap Modal.

El escaneo incluye tanto `.js` como scripts incrustados en `.html`. Resultado: cero llamadas a `alert()`, `confirm()` o `prompt()` en los recursos de aplicación. Se añadió una prueba que falla si vuelven a introducirse.

## 6. Verificación ejecutada

- **59 pruebas Java: aprobadas; 0 fallos, 0 errores, 0 omitidas.** Incluyen contexto completo con base H2 aislada, servicios, controladores y renderizado Thymeleaf.
- **13 pruebas JavaScript: aprobadas.** Registro, OTP, recuperación, checkout real simulado, reintento con misma clave, conservación del carrito ante errores, prohibición de diálogos y referencias de fragmentos.
- El checkout se prueba con transacciones reales de Spring/JPA: fallo posterior a guardar pedido revierte productos, insumos, pedido y detalles; stock de insumos insuficiente también revierte productos.
- Dos clientes compiten por la última unidad: se confirma exactamente un pedido y el stock queda en cero.
- Se comprueba que repetir una clave no vuelve a descontar existencias; fallo SMTP conserva compra y fila pendiente.
- Se comprueban eliminación atómica de cuenta, saldo de cupones recalculado y espera entre reenvíos OTP.
- Se comprueban respuestas 401/403 de administración, rechazo de mutación desde otro origen y 405 al intentar borrar una pizza por GET.
- HTML de boleta probado con caracteres escapados, importes y destinatario mediante mensaje MIME controlado.

Los errores SMTP que aparecen en los logs de los tests son fallos provocados deliberadamente. No se enviaron mensajes reales. H2 valida las transacciones y consultas en el entorno local, pero no reproduce todas las características, planes ni bloqueos de PostgreSQL. La migración SQL no fue ejecutada sobre Neon. No se realizó inspección visual en navegador.

Comandos usados desde `Caleta_Guillermo/lacaleta`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd -o '-DargLine=-javaagent:C:\Users\grstu\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar' test
node --test src/test/js/account-verification.test.cjs src/test/js/checkout-ui.test.cjs
```

El proyecto compila para Java 17; se ejecutaron las pruebas con el JDK 21 instalado. H2 es una dependencia solo de pruebas. El `javaagent` corresponde a Mockito disponible en este equipo.

## 7. Pasos necesarios antes de ejecutar con servicios reales

1. Rotar las credenciales de Neon y SMTP que estuvieron escritas en archivos. Cambiar el archivo actual no elimina valores del historial de Git. También hay archivos generados y un archivo de cookies ya versionados; `.gitignore` impide nuevas incorporaciones, pero los archivos previamente rastreados necesitan una limpieza del repositorio y de su historial según corresponda.
2. Exportar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME` y `MAIL_PASSWORD`. Ver `.env.example`: Spring Boot no carga ese archivo automáticamente.
3. Probar `db/auditoria-2026-10-16.sql` en una rama aislada de Neon con conexión directa. El script complementa las tablas originales: añade versiones, clave de idempotencia, índices, recetas y outbox. No contiene un esquema inicial completo para una base vacía. Revisar que la rama ya contenga las tablas/campos de los avances fusionados, incluidos usuarios, cupones y verificaciones.
4. Mantener `DDL_AUTO=validate` en el entorno compartido para detectar divergencias sin cambiar el esquema al arrancar. Para una primera instalación de desarrollo puede generarse el esquema desde los modelos con `DDL_AUTO=update` en una base aislada, y después volver a `validate`. El perfil `demo` es opcional y solo carga datos de demostración.
5. Configurar las recetas reales en el panel. Los productos preparados sin receta son rechazados deliberadamente, para que nunca se descuenten solo pizzas sin consumir sus insumos.
6. Arrancar y realizar una compra de prueba en la rama: comprobar cantidades antes/después, reintento del mismo intento, historial del usuario y envío de la boleta por el SMTP configurado. Verificar también el comportamiento del trabajador y sus permisos sobre `boletas_pendientes`.

La conexión MCP de Neon disponible no estaba asociada a un proyecto y exigió `project_id`; no se pudo identificar el proyecto remoto desde esa conexión. No se crearon proyectos/ramos ni se aplicaron DDL/DML remotos. Por eso las conclusiones sobre la base efectiva, SMTP y el arranque con la configuración del despliegue quedan pendientes de esa verificación.

La UI original recogía datos de tarjeta y un código Yape, pero no había una pasarela ni validación de esos pagos. Se retiraron esos formularios engañosos y la opción de factura sin implementación; el flujo disponible registra pedidos pendientes con pago al entregar o retirar. Integrar cobro electrónico o emisión electrónica requiere un trabajo adicional. El correo HTML implementado es un comprobante interno del pedido.

## Archivos principales para revisión

- [Checkout transaccional](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/src/main/java/com/lacaleta/service/PedidoService.java:37>).
- [Outbox y reintentos de correo](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/src/main/java/com/lacaleta/service/BoletaOutboxService.java:18>).
- [Protección administrativa](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/src/main/java/com/lacaleta/config/AdminInterceptor.java:13>).
- [Historial paginado](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/src/main/java/com/lacaleta/controller/PageController.java:229>).
- [Migración SQL pendiente de aplicar](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/db/auditoria-2026-10-16.sql>).
- [Pruebas de integración transaccional](<C:/Users/grstu/Documents/ciclo 6/Integrador 1/Semana 10 jueves/caleta-guille/Caleta_Guillermo/lacaleta/src/test/java/com/lacaleta/service/CheckoutIntegrationTests.java>).

Se generó también el JAR ejecutable en `Caleta_Guillermo/lacaleta/target/lacaleta-0.0.1-SNAPSHOT.jar`. Después de completar configuración y esquema, puede iniciarse desde la carpeta del proyecto con `java -jar target/lacaleta-0.0.1-SNAPSHOT.jar` (puerto configurado: 8082). El empaquetado se verifica con Maven; la entrega del JAR no acredita una conexión real a Neon/SMTP.
