# Demostración de La Caleta

Configuración aplicada el 16 de octubre de 2026.

## Estado aplicado en Neon

- Proyecto usado por la aplicación: **la-caleta-db** (`summer-forest-76955823`).
- Rama de demostración: **demo-caleta** (`br-dark-sound-ayu8ebz7`).
- [Abrir la rama en Neon](https://console.neon.tech/app/projects/summer-forest-76955823/branches/br-dark-sound-ayu8ebz7).
- La rama `production` se conservó sin aplicar la migración ni los datos ficticios.
- Migración de auditoría ejecutada y comprobada también desde el script JDBC local.
- **24 productos preparados con receta, 223 relaciones producto–insumo y cero preparados disponibles sin receta.**
- **22 insumos de demostración**; hay 24 insumos en total porque se conservaron dos adicionales del catálogo anterior.
- Las siete bebidas conservan el descuento de su stock por SKU. Las recetas de combos son consolidadas por unidad del combo; no descuentan por separado otros SKU de pizza o bebida.

La rama vacía creada inicialmente en el proyecto homónimo `Pizzeria-Caleta` se eliminó con autorización del usuario. La demo activa está únicamente en el proyecto indicado arriba.

## Preparar y arrancar de una vez

Desde esta carpeta, con Node.js y un JDK 17 o superior:

```powershell
.\Preparar-Demo.ps1 -Iniciar
```

El script regenera el SQL, aplica migración y datos en una transacción usando la conexión directa y después inicia Spring Boot. La aplicación queda en [http://localhost:8082](http://localhost:8082). Para preparar sin arrancar:

```powershell
.\Preparar-Demo.ps1
```

La instalación ya se aplicó. Repetir el comando no duplica insumos ni recetas, no reinicia el stock consumido y conserva una receta editada por el administrador. La primera instalación eleva el inventario de los ingredientes reutilizados al mínimo ficticio definido; nunca reduce las existencias previas.

El SQL complementa el esquema existente copiado de la aplicación; no es un instalador de todas las tablas para una base completamente vacía. Está destinado a la rama de demostración. Si agregas un alimento nuevo sin receta, el script falla y revierte la transacción, en lugar de dejar una instalación incompleta.

## Archivos y propósito

| Archivo | Uso |
| --- | --- |
| `db/auditoria-2026-10-16.sql` | Migración de versiones, idempotencia de pedidos, índices, recetas y outbox. |
| `db/generar-demo.cjs` | Fuente de insumos y recetas ficticias; genera los dos archivos siguientes sin conectarse a la base. |
| `db/preparar-demo.sql` | Instalador SQL único: migración y carga inicial dentro de una transacción; usa un bloqueo para serializar instalaciones concurrentes. |
| `db/recetas-demo.json` | Listado legible de stock inicial, unidades y cantidades por producto. |
| `scripts/DemoDatabase.java` | Ejecuta el instalador por JDBC, sin requerir `psql` ni imprimir credenciales. |
| `Preparar-Demo.ps1` | Prepara la base y, con `-Iniciar`, arranca la aplicación. |
| `Verificar-Demo.ps1` | Ejecuta ambos checkouts reales en transacciones que se revierten y comprueba opcionalmente la autenticación SMTP. |
| `src/main/java/com/lacaleta/demo/DemoCheckRunner.java` | Verificación del catálogo completo, descuento exacto de insumos/productos, importe, modalidad y boleta HTML. Solo se activa con el perfil `demo-check`. |
| `.env` | Configuración privada local, ya creada e ignorada por Git. No compartir ni versionar. |
| `.env.example` | Plantilla sin secretos para otra instalación. |

## Inventario ficticio

| Insumo | Stock inicial de referencia | Mínimo |
| --- | --- | --- |
| Harina de trigo | 80 kg | 10 kg |
| Queso mozzarella | 40 kg | 5 kg |
| Salsa de tomate | 35 kg | 5 kg |
| Pepperoni | 15 kg | 2 kg |
| Jamón | 15 kg | 2 kg |
| Piña | 12 kg | 2 kg |
| Tocino | 12 kg | 2 kg |
| Carne de res | 12 kg | 2 kg |
| Pimiento | 8 kg | 1 kg |
| Champiñones | 8 kg | 1 kg |
| Aceituna | 5 kg | 1 kg |
| Albahaca | 1 kg | 0,2 kg |
| Aceite de oliva | 8 litros | 1 litro |
| Levadura | 2 kg | 0,25 kg |
| Sal | 5 kg | 0,5 kg |
| Agua filtrada | 100 litros | 10 litros |
| Mantequilla | 6 kg | 1 kg |
| Ajo | 3 kg | 0,5 kg |
| Queso parmesano | 5 kg | 0,5 kg |
| Alitas de pollo | 20 kg | 3 kg |
| Salsa BBQ | 8 kg | 1 kg |
| Queso fresco | 10 kg | 2 kg |

Si un ingrediente ya existía, se reutilizó por nombre y se conservaron sus datos administrativos. La columna de mínimo de esta tabla corresponde a los ingredientes nuevos; los mínimos previamente editados se conservan. Por ejemplo, el tocino ya tenía 40 kg: se mantuvieron esos 40 kg.

Cada pizza familiar comparte esta base: 300 g de harina, 180 ml de agua, 5 g de levadura, 6 g de sal, 10 ml de aceite, 200 g de mozzarella y 120 g de salsa de tomate.

| Pizza | Ingredientes adicionales por unidad familiar |
| --- | --- |
| La Cómplice | 90 g de pepperoni |
| El Pacto | 80 g de jamón y 60 g de tocino |
| La Coartada | 80 g de jamón y 100 g de piña |
| La Confesión | 50 g de pimiento, 60 g de champiñones, 25 g de aceituna y 3 g de albahaca |
| La Cofradía | 60 g de pepperoni, 80 g de carne y 50 g de tocino |

Las personales consumen el 45 % de la receta familiar. Los combos y promociones usan sumas de recetas según sus porciones ficticias; las cantidades exactas están en `db/recetas-demo.json`. Para la demo, las pizzas a elección de los combos tienen una combinación fija de ingredientes definida en ese archivo. Las modificaciones posteriores se hacen con el botón **Receta** en administración.

## Variables y correo

La aplicación importa el `.env` local desde `application.properties` con `spring.config.import=optional:file:.env[.properties]`. Es un archivo en formato Java Properties: no requiere `export` ni ejecutar su contenido como código. [Documentación de configuración externa de Spring Boot](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/features/external-config.html).

Variables principales:

- `DATABASE_URL`: URL **JDBC** con el endpoint pooled de la rama de demostración.
- `DATABASE_DIRECT_URL`: conexión directa para el script SQL.
- `DATABASE_USERNAME` y `DATABASE_PASSWORD`: credenciales separadas de la URL.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`: SMTP.
- `DDL_AUTO=validate`: el arranque comprueba el esquema sin modificarlo automáticamente.

Las variables de entorno del proceso pueden sustituir los valores del archivo. Las antiguas variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` siguen admitidas como alternativa en la aplicación. En `.env` se reutilizó la configuración SMTP local anterior, sin copiarla a archivos versionables. Cambiar a variables no elimina credenciales del historial: las claves expuestas antes deben rotarse antes de producción.

El trabajador normal de boletas está habilitado. Guarda el HTML junto con el pedido y lo envía después del commit, con reintentos ante errores SMTP. No necesita pasarela de pagos. Las cuentas siguen requiriendo validación de correo por OTP; no se marcaron usuarios copiados como verificados. Los administradores pendientes deberán completar su verificación antes de acceder al panel.

## Pago disponible

| Entrega | Modalidad guardada | Envío | Estado inicial |
| --- | --- | --- | --- |
| Delivery | `CONTRA_ENTREGA` — Pago contra entrega | S/ 5,00 | `PENDIENTE` |
| Retiro | `PAGO_EN_TIENDA` — Pago al retirar en tienda | S/ 0,00 | `PENDIENTE` |

El backend rechaza modalidades incompatibles y pagos de pasarelas no implementadas. Por compatibilidad, si un cliente antiguo manda `EFECTIVO`, se normaliza a la modalidad correspondiente a la entrega. La boleta muestra que el pago está pendiente y la dirección o retiro. Registrar el pedido no lo marca como cobrado.

## Verificación realizada

```powershell
.\Verificar-Demo.ps1 -ComprobarSmtp
```

Resultado sobre PostgreSQL 18 de Neon:

- Checkout con una unidad de cada uno de los **31 productos disponibles**, en ambas modalidades.
- **22 insumos** descontados exactamente según las recetas acumuladas, incluyendo los ingredientes compartidos entre productos.
- Delivery: **S/ 1213,60**; retiro: **S/ 1208,60**. Precios obtenidos del catálogo de la rama al comprobarlo.
- Pedido, detalles y boleta HTML persistidos dentro de cada transacción de prueba.
- Rollback posterior comprobado: no quedaron pedidos, boletas ni descuentos de stock de las pruebas.
- Conexión y autenticación SMTP reales: correctas. No se enviaron correos de prueba; no se comprobó recepción en una bandeja de correo.
- En este modo de verificación el trabajador de correo está desactivado para no despachar mensajes. El arranque normal lo habilita.
- **61 pruebas Java y 14 JavaScript aprobadas**, sin fallos. JAR regenerado correctamente con Maven `verify`.

Para otra máquina, generar primero el JAR con `mvnw.cmd package` y configurar el `.env`. El script usa el JDK indicado por `JAVA_HOME`; en este equipo detecta también el JDK 21 instalado. Las pruebas de checkout necesitan al menos un cliente con correo verificado, existencias suficientes y como máximo 100 productos disponibles. Las transacciones de verificación consumen números de secuencia, por lo que los IDs pueden presentar saltos aunque los pedidos se reviertan.
