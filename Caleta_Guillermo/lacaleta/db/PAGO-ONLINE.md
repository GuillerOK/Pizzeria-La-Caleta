# Pago simulado y cupones — 21/10/2026

## Pasarela de demostración

`/pago` incluye tarjeta de 16 dígitos, titular, vencimiento MM/YY vigente y CVV de 3 o 4 dígitos. El botón «Usar datos de prueba» completa valores ficticios. Ningún dato de tarjeta se transmite ni se almacena.

Al pagar se bloquea el formulario, aparece el spinner y se esperan dos segundos. Después se invoca `/api/checkout` con `ONLINE_SIMULADO`, conservando la clave de idempotencia. Solo una respuesta con un pedido confirmado permite vaciar el carrito y redirigir a `/pago/exitoso?pedidoId=...`. La vista consulta el pedido persistido y exige que pertenezca a la sesión y esté realizado o entregado. Los errores conservan carrito y cupón para reintentar.

El JavaScript de pago tiene una URL versionada para evitar que una copia antigua siga enviando métodos de pago anteriores. Después de actualizar, recompilar el JAR y reiniciar el servidor; no mezclar recursos compilados antiguos con las fuentes actuales.

Esta actualización no requiere migración de esquema. Se verificaron en Neon las columnas y restricciones de `pedidos` y `cupones` en la rama configurada por la aplicación. `estado` y `metodo_pago` admiten los nuevos valores; `codigo` conserva sus 50 caracteres y su restricción única.

Las nuevas compras aceptan únicamente `ONLINE_SIMULADO` y persisten `REALIZADO` tras las validaciones del servidor. Pago simulado, detalles, stock, puntos y consumo del cupón forman parte de la misma transacción. No hay cobro real. Un reintento con la misma clave devuelve el pedido confirmado sin volver a consumir stock ni cupón.

No se modifican pedidos históricos ni códigos existentes. Los códigos nuevos usan el prefijo del beneficio seguido de ocho caracteres legibles, por ejemplo `CAL5-K7M9W3RX`. Se verifica su disponibilidad y se mantiene la restricción única de la base de datos. Los códigos largos continúan validándose sin truncar ni limitar su longitud en la interfaz.

El perfil y el modal del carrito comparten `fragments/cupones`. Seleccionar, copiar o validar no consume cupones. El código seleccionado sobrevive a otras páginas y al carrito vacío; se limpia al aplicar o al completar la compra. El cupón aplicado solo se marca `USADO` cuando la compra se confirma.

Dashboard y reportes suman `REALIZADO` y `ENTREGADO`, excluyendo pendientes y cancelados. Ambos estados exitosos se muestran en verde. Para usar los cambios, compilar y reiniciar la aplicación con su configuración habitual de Neon y SMTP. La boleta conserva el envío síncrono existente; un error SMTP revierte la compra (véase `CUPONES.md`).
