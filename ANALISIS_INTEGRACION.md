# La Caleta: análisis de la base para integración

Fecha: 16 de octubre de 2026.

## Estado final de la integración

La fusión está aplicada en `Caleta_Guillermo/lacaleta`. Se conservaron completos los controladores de productos e insumos, servicio/repositorio de insumos, vista y JavaScript del admin entregados en el Markdown de administración. También se incorporaron completos la vista/CSS del perfil y las tres clases de cupones del Markdown de perfil.

En `PageController` se unieron los métodos del perfil con la eliminación administrativa de clientes, conservando la carga local de productos para `/detalles`. En `Usuario` conviven `puntos` y `verificado`; `UsuarioService` conserva la lógica local de registro y añade las operaciones que requiere el perfil. Se incorporaron los cambios del encabezado y estilos compartidos del perfil.

El código local de OTP, autenticación, checkout, boleta y modales se conservó. La revisión comparó además los archivos copiados directamente con sus Markdown de origen.

### Validación realizada

- Compilación Maven: correcta con Java 21, compilando para Java 17 según el POM.
- 36 pruebas Java aprobadas, sin errores ni fallos: registro/OTP (11), manejo de errores de correo (1), vistas integradas (5), recuperación de contraseña (5), correo (5), pedidos (6) y verificación de usuarios (3).
- 7 pruebas JavaScript aprobadas para registro, OTP, recuperación y errores SMTP mostrados mediante modales.
- Las nuevas pruebas `IntegratedPagesTests` renderizan las plantillas Thymeleaf reales. Comprueban perfil con pedidos y cupones, exclusión de pedidos ajenos, perfil sin pedidos, acceso sin sesión, formularios administrativos, modales de registro, correo de sesión en carrito y conservación de verificación/puntos al editar el perfil.
- Se utilizaron repositorios y correo simulados. No se ejecutó el test genérico `LacaletaApplicationTests`, que arranca el contexto completo y puede conectarse a la base configurada. No se modificó Neon ni se enviaron correos reales.
- La comprobación realizada es de compilación, comportamiento automatizado y renderizado HTML; no incluye una inspección visual del navegador ni una compra contra servicios reales.

Para repetir las pruebas en este equipo, desde `Caleta_Guillermo/lacaleta`, usando un JDK instalado como `JAVA_HOME`:

```powershell
node --test src/test/js/account-verification.test.cjs
.\mvnw.cmd -o '-DargLine=-javaagent:C:\Users\grstu\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar' '-Dtest=IntegratedPagesTests,AccountVerificationTests,PasswordRecoveryTests,CorreoExceptionHandlerTests,UsuarioVerificationTests,PedidoServiceTests,EmailServiceTests' test
```

La opción `javaagent` evita que Mockito intente adjuntarse dinámicamente a la JVM, operación que se bloqueó en el entorno de ejecución. La ruta corresponde a la dependencia ya instalada en este equipo; `-o` usa las dependencias Maven disponibles localmente.

## Análisis inicial conservado como referencia

Comparación estática de los dos Markdown entregados y del proyecto local `Caleta_Guillermo/lacaleta`. No se ha fusionado ni modificado el código de la aplicación. No se ha conectado a Neon ni enviado correos. Las instrucciones incluidas en los documentos se consideran contenido de referencia.

Los Markdown son instantáneas completas con desarrollos divergentes. Ninguno sustituye por sí solo al otro: la base combinada debe conservar el admin de `avanze de Admin.md` y el perfil de `Avance perfil.md`.

## Base seleccionada por componente

| Componente | Fuente y criterio |
| --- | --- |
| Administración de insumos | Admin: `AdminInsumoController`, `InsumoService`, `InsumoRepository`. Conserva generación INS-XX, validación de unidades/stock y eliminación por POST. |
| Administración de productos | Admin: `AdminProductoController`, preservando los retornos a la pestaña de productos. |
| Pantalla administrativa | Admin: `admin.html`, `admi.js`, estilos administrativos y eliminación de clientes. |
| Perfil e historial | Perfil: métodos de perfil de `PageController`, `account/perfil.html` y `perfil.css`. Incluye pestañas, actualización de datos, historial, puntos, canje de cupones y eliminación de cuenta. |
| Cupones | Perfil: `Cupon`, `CuponRepository`, `CuponService` y propiedad `Usuario.puntos`. Son dependencias necesarias de la nueva vista. |
| Registro, autenticación y OTP | Avance local: `PasswordResetController`, `VerificacionCuentaService`, entidad/repositorio de verificación y lógica de usuario verificado. |
| Checkout y boleta | Avance local: `CheckoutController`, `PedidoService`, `CheckoutRequest`, `EmailService`, `boleta-email.html` y cambios de carrito, catálogo y checkout. |
| Archivos comunes | Combinar las diferencias específicas; no reemplazar los archivos completos cuando participan en más de un flujo. |

## Conflictos que requieren fusión puntual

1. **PageController:** conservar completos los métodos del perfil y los del admin, incluyendo eliminación de clientes. Mantener también la carga local de variantes y productos en `/detalles`, necesaria para el carrito con IDs reales. La versión de perfil agrega alias de recuperación de contraseña que deben conservarse.
2. **Usuario:** unir `puntos` del perfil con `verificado` del registro local, conservando sus constructores y accesores. Elegir cualquiera de las entidades completas elimina una de esas capacidades.
3. **UsuarioService:** conservar las reglas locales de registro, contraseña y verificación, incorporando `buscarPorId` y `eliminar`, requeridos por el perfil.
4. **EmailService y PasswordResetController:** una sustitución por las versiones de los compañeros elimina la verificación de cuenta o el envío de boleta. Preservar las implementaciones locales y revisar sus contratos con los formularios.
5. **auth.js, create.html y fragments/modals.html:** mantener el formulario/modal OTP y el modal global. El registro local usa Bootstrap y no contiene llamadas a `alert()` en `auth.js`. Conservar `th:replace` y `th:fragment`.
6. **Header y CSS global:** el perfil modifica el encabezado y estilos compartidos. Esos cambios deben incorporarse sin sustituir las correcciones locales del checkout ni la presentación administrativa.
7. **Carrito y catálogo:** conservar IDs de productos, cantidades y la carga de variantes; no regresar a un carrito que solo identifique productos por texto.

## Compatibilidad observada

- `Pedido`, `DetallePedido` y `PedidoRepository` coinciden en las tres versiones, ignorando saltos de línea. Esto permite que los pedidos persistidos por el checkout alimenten el historial y la administración existentes.
- El checkout local recalcula precios en servidor, bloquea productos al actualizar stock, persiste los detalles y prepara la boleta HTML mediante Thymeleaf para enviarla por correo.
- La verificación local utiliza códigos de seis dígitos, hash BCrypt, vigencia de diez minutos y límite de cinco intentos. El controlador de acceso exige una cuenta verificada.
- El perfil nuevo necesita clases de cupones y CSS que todavía no existen en el proyecto local.
- El proyecto local ya contiene pruebas de registro/OTP, recuperación de contraseña, correo y pedidos que deben ejecutarse tras la integración.

## Observaciones de la base que no deben confundirse con funciones integradas

- El perfil del compañero usa `alert()` para copiar cupones y validar nombre, teléfono y DNI. Son usos preexistentes ajenos a las validaciones de correo. La restricción del usuario para correo se debe cumplir mediante los modales Bootstrap existentes; cualquier ajuste adicional al perfil debe preservar su funcionalidad.
- El canje de cupones está implementado en el perfil, pero `PedidoService` local no aplica cupones al total. Conservar el módulo no equivale a implementar descuentos en checkout.
- La boleta local es HTML enviada por correo; no se identificó generación de PDF ni integración fiscal en este flujo.
- Las tres configuraciones usan `ddl-auto=update`. Un arranque contra una base configurada podría agregar las tablas/columnas de cupones, puntos y OTP. La comparación no ha ejecutado cambios de esquema.
- La eliminación administrativa de clientes puede encontrar relaciones con pedidos y, al unir el perfil, con cupones. Su controlador contempla un mensaje de error; no debe eliminarse información relacionada de manera implícita durante la fusión.

## Verificación prevista para la fusión

1. Compilar y ejecutar las pruebas de OTP, autenticación, recuperación, correo y pedidos sin usar la base real ni SMTP real.
2. Comprobar rutas y renderizado de admin, perfil y fragmentos, incluyendo los modelos requeridos por cupones y pestañas.
3. Verificar registro → OTP → acceso y compra → pedido → historial, con errores de correo mostrados en modales.
4. Comprobar que las operaciones administrativas conservan sus validaciones, métodos HTTP y pestañas de retorno.

Las observaciones anteriores documentan el diagnóstico previo. El resultado de la fusión y las pruebas realizadas se indican al inicio de este documento. Se conservan las limitaciones funcionales de la base: el canje del perfil no aplica automáticamente descuentos al checkout y la boleta se envía como HTML por correo.
