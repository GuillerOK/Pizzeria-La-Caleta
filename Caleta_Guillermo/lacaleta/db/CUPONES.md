# Despliegue de cupones y saldo de puntos

Ejecutar `cupones-checkout-2026-10-19.sql` antes de iniciar esta versión, porque Hibernate valida el esquema. La migración es aditiva e idempotente; no reconstruye importes históricos ni modifica saldos de usuarios.

Los nuevos canjes de gaseosa vinculan `Inca Kola 1.5L` y los de pizza `La Cómplice Personal`, productos del catálogo inicial. Estos nombres deben ser únicos. La relación `cupones.producto_regalo_id` conserva el producto seleccionado aunque después cambie su nombre. La consulta final de la migración identifica regalos antiguos que requieren asignación manual. Un regalo sin producto o sin stock no se puede comprar.

El saldo `usuarios.puntos` se lee sin reconstruirlo. Los canjes descuentan una sola vez bajo bloqueo y los pedidos acreditan el incremento de puntos por consumo acumulado (1 por cada S/ 5). No se recuperan automáticamente puntos perdidos antes de la corrección: esa reparación requiere contrastar el historial y cualquier ajuste manual previo.

Las compras nuevas envían la boleta sincrónicamente dentro de la transacción; una excepción de correo revierte pedido, cupón, puntos, productos e insumos. El procesador de boletas pendientes permanece para correos de versiones anteriores, pero las compras nuevas no se encolan.

SMTP no participa en la transacción de PostgreSQL. Si Gmail acepta un mensaje y luego falla el commit, o se pierde la respuesta SMTP, no es posible retirar ese mensaje. La clave de compra evita repetir compras ya confirmadas, pero no elimina esta limitación de atomicidad entre dos sistemas.

Pruebas locales: `mvnw.cmd test` (perfil test con H2 y correo simulado, sin cambios en Neon ni correos reales).
