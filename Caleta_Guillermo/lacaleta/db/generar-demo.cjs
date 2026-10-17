// Datos ficticios para la presentación. Solo genera SQL; no abre conexiones.
const fs = require('node:fs');
const path = require('node:path');
const ingredientes = [
    ['HARINA', 'Harina de Trigo', 80, 'kg', 10],
    ['MOZZARELLA', 'Queso Mozzarella', 40, 'kg', 5],
    ['TOMATE', 'Salsa de Tomate', 35, 'kg', 5],
    ['PEPPERONI', 'Pepperoni Laminado', 15, 'kg', 2],
    ['JAMON', 'Jamón', 15, 'kg', 2],
    ['PINA', 'Piña', 12, 'kg', 2],
    ['TOCINO', 'Tocino', 12, 'kg', 2],
    ['CARNE', 'Carne de Res', 12, 'kg', 2],
    ['PIMIENTO', 'Pimiento', 8, 'kg', 1],
    ['CHAMPINON', 'Champiñones', 8, 'kg', 1],
    ['ACEITUNA', 'Aceituna', 5, 'kg', 1],
    ['ALBAHACA', 'Albahaca', 1, 'kg', 0.2],
    ['ACEITE', 'Aceite de Oliva', 8, 'litros', 1],
    ['LEVADURA', 'Levadura', 2, 'kg', 0.25],
    ['SAL', 'Sal', 5, 'kg', 0.5],
    ['AGUA', 'Agua Filtrada', 100, 'litros', 10],
    ['MANTEQUILLA', 'Mantequilla', 6, 'kg', 1],
    ['AJO', 'Ajo', 3, 'kg', 0.5],
    ['PARMESANO', 'Queso Parmesano', 5, 'kg', 0.5],
    ['POLLO', 'Alitas de Pollo', 20, 'kg', 3],
    ['BBQ', 'Salsa BBQ', 8, 'kg', 1],
    ['FRESCO', 'Queso Fresco', 10, 'kg', 2]
];
const base = { HARINA: .30, AGUA: .18, LEVADURA: .005, SAL: .006, ACEITE: .01, MOZZARELLA: .20, TOMATE: .12 };
const sumar = (...recetas) => recetas.reduce((total, receta) => {
    for (const [insumo, cantidad] of Object.entries(receta)) total[insumo] = (total[insumo] || 0) + cantidad;
    return total;
}, {});
const escalar = (receta, factor) => Object.fromEntries(Object.entries(receta).map(([i, q]) => [i, q * factor]));
const complice = sumar(base, { PEPPERONI: .09 });
const pacto = sumar(base, { JAMON: .08, TOCINO: .06 });
const coartada = sumar(base, { JAMON: .08, PINA: .10 });
const confesion = sumar(base, { PIMIENTO: .05, CHAMPINON: .06, ACEITUNA: .025, ALBAHACA: .003 });
const cofradia = sumar(base, { PEPPERONI: .06, CARNE: .08, TOCINO: .05 });
const palitos = { HARINA: .15, AGUA: .09, LEVADURA: .003, SAL: .003, MANTEQUILLA: .03, AJO: .01, PARMESANO: .025 };
const alitas = { POLLO: .45, BBQ: .08, SAL: .004 };
const tequenos = { HARINA: .15, AGUA: .05, SAL: .003, FRESCO: .16, ACEITE: .025 };
const recetas = {
    'La Cómplice': complice, 'El Pacto': pacto, 'La Coartada': coartada,
    'La Confesión': confesion, 'La Cofradía': cofradia,
    'La Cómplice Personal': escalar(complice, .45),
    'El Pacto Personal': escalar(pacto, .45), 'La Coartada Personal': escalar(coartada, .45),
    'Palitos de Ajo': palitos, 'Alitas BBQ': alitas, 'Tequeños de Queso': tequenos,
    'Dúo Clandestino': escalar(complice, 1.7),
    'Trío en Secreto': sumar(escalar(complice, .7), escalar(pacto, .7), escalar(coartada, .7)),
    'El Silencio': sumar(complice, palitos),
    'Pacto Caleta': sumar(complice, pacto, alitas),
    'Dúo Cómplice': escalar(complice, 1.4), 'Dúo Pacto Clásico': escalar(pacto, 1.4),
    'Trilogía Caleta': sumar(escalar(complice, .7), escalar(pacto, .7), escalar(coartada, .7)),
    'Banquete Clandestino': sumar(complice, coartada, alitas),
    'Trío con Acompañamiento': sumar(escalar(complice, .7), escalar(pacto, .7), escalar(coartada, .7), palitos),
    'Dúo Carnívoro': escalar(cofradia, 1.4), 'Fiesta en La Caleta': sumar(complice, pacto, coartada),
    'Trío Cómplices': escalar(complice, 2.1), 'Gran Cofradía Familiar': escalar(cofradia, 1.5)
};
const literal = valor => "'" + String(valor).replaceAll("'", "''") + "'";
const filasInsumos = ingredientes.map(([codigo, nombre, stock, unidad, minimo]) =>
    `(${literal(codigo)}, ${literal(nombre)}, ${stock}, ${literal(unidad)}, ${minimo})`).join(',\n');
const filasRecetas = Object.entries(recetas).flatMap(([nombre, receta]) =>
    Object.entries(receta).map(([insumo, cantidad]) => `(${literal(nombre)}, ${literal(insumo)}, ${cantidad.toFixed(6)})`)).join(',\n');
const seed = `-- DEMOSTRACIÓN: cantidades ficticias por unidad de venta. No reinicia stock al repetir.
CREATE TABLE IF NOT EXISTS app_demo_instalaciones (nombre varchar(100) PRIMARY KEY, aplicado_en timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TEMP TABLE demo_insumos (codigo text, nombre text, stock double precision, unidad text, minimo double precision) ON COMMIT DROP;
INSERT INTO demo_insumos VALUES
${filasInsumos};
INSERT INTO insumos(codigo,nombre,stock_actual,unidad_medida,stock_minimo,version)
SELECT 'DEMO-' || d.codigo,d.nombre,d.stock,d.unidad,d.minimo,0 FROM demo_insumos d
WHERE NOT EXISTS (SELECT 1 FROM insumos i WHERE lower(trim(i.nombre))=lower(d.nombre))
ON CONFLICT (codigo) DO NOTHING;
-- Solo la primera instalación establece existencias mínimas de demostración.
UPDATE insumos i SET stock_actual=greatest(i.stock_actual,d.stock), version=coalesce(i.version,0)+1
FROM demo_insumos d WHERE lower(trim(i.nombre))=lower(d.nombre) AND lower(i.unidad_medida)=d.unidad
AND NOT EXISTS (SELECT 1 FROM app_demo_instalaciones WHERE nombre='recetas-demo-v1');
CREATE TEMP TABLE demo_recetas (producto text, insumo text, cantidad numeric(19,6)) ON COMMIT DROP;
INSERT INTO demo_recetas VALUES
${filasRecetas};
-- Una receta editada por el administrador se conserva completa.
WITH productos_sin_receta AS (
 SELECT p.id,p.nombre FROM productos p WHERE NOT EXISTS (SELECT 1 FROM receta_insumos r WHERE r.producto_id=p.id)
)
INSERT INTO receta_insumos(producto_id,insumo_id,cantidad)
SELECT p.id, (SELECT min(i.id) FROM insumos i WHERE lower(trim(i.nombre))=lower(d.nombre)), r.cantidad
FROM productos_sin_receta p JOIN demo_recetas r ON lower(trim(p.nombre))=lower(r.producto)
JOIN demo_insumos d ON d.codigo=r.insumo
ON CONFLICT (producto_id,insumo_id) DO NOTHING;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM productos p JOIN categorias c ON c.id=p.categoria_id
   WHERE p.disponible AND lower(c.codigo) NOT IN ('bebida','bebidas')
   AND NOT EXISTS (SELECT 1 FROM receta_insumos r WHERE r.producto_id=p.id)) THEN
   RAISE EXCEPTION 'Hay productos preparados sin receta. Añadirlos al catálogo de db/generar-demo.cjs antes de continuar.';
 END IF;
END $$;
INSERT INTO app_demo_instalaciones(nombre) VALUES ('recetas-demo-v1') ON CONFLICT DO NOTHING;
`;
const auditoria = fs.readFileSync(path.join(__dirname, 'auditoria-2026-10-16.sql'), 'utf8')
    .replace(/^BEGIN;\s*/m, '').replace(/^COMMIT;\s*/m, '');
fs.writeFileSync(path.join(__dirname, 'preparar-demo.sql'),
    '-- Generado por node db/generar-demo.cjs. Aplicar SOLO en una base de demostración.\nBEGIN;\nSELECT pg_advisory_xact_lock(20261016, 1);\n'
    + auditoria + '\n' + seed + '\nCOMMIT;\n', 'utf8');
fs.writeFileSync(path.join(__dirname, 'recetas-demo.json'), JSON.stringify({
    aviso: 'Datos ficticios de demostración. Cantidades por SKU vendido; combos tienen receta consolidada.',
    ingredientes: ingredientes.map(([codigo, nombre, stockInicial, unidad, stockMinimo]) => ({ codigo, nombre, stockInicial, unidad, stockMinimo })),
    recetas: Object.fromEntries(Object.entries(recetas).map(([nombre, receta]) =>
        [nombre, Object.fromEntries(Object.entries(receta).map(([insumo, cantidad]) => [insumo, Number(cantidad.toFixed(6))]))]))
}, null, 2) + '\n', 'utf8');
console.log(`SQL generado: ${ingredientes.length} insumos y ${Object.keys(recetas).length} recetas.`);
