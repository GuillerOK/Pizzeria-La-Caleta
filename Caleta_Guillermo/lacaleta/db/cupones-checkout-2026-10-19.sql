-- Ejecutar antes de desplegar (la aplicación usa ddl-auto=validate).
BEGIN;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS subtotal double precision;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS envio double precision;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS descuento_cupon double precision;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS codigo_cupon varchar(255);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS tipo_cupon varchar(255);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS titulo_cupon varchar(255);
ALTER TABLE detalle_pedidos ADD COLUMN IF NOT EXISTS cupon_regalo boolean NOT NULL DEFAULT false;
ALTER TABLE cupones ADD COLUMN IF NOT EXISTS producto_regalo_id integer REFERENCES productos(id);

-- Vincular los regalos antiguos del catálogo a productos concretos sin modificar puntos.
-- Solo asociar nombres inequívocos; no elegir arbitrariamente entre duplicados.
UPDATE cupones c SET producto_regalo_id = p.id
FROM (SELECT min(id) AS id FROM productos WHERE lower(nombre) = lower('Inca Kola 1.5L') HAVING count(*) = 1) p
WHERE c.tipo = 'REGALO' AND c.codigo LIKE 'BEB-%' AND c.producto_regalo_id IS NULL;
UPDATE cupones c SET producto_regalo_id = p.id
FROM (SELECT min(id) AS id FROM productos WHERE lower(nombre) = lower('La Cómplice Personal') HAVING count(*) = 1) p
WHERE c.tipo = 'REGALO' AND c.codigo LIKE 'PIZZA-%' AND c.producto_regalo_id IS NULL;
COMMIT;

-- Debe devolver cero filas; resolver manualmente cualquier regalo sin producto.
SELECT id, codigo, titulo FROM cupones
WHERE tipo = 'REGALO' AND estado = 'ACTIVO' AND producto_regalo_id IS NULL;
