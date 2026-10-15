package com.lacaleta;

import com.lacaleta.model.Categoria;
import com.lacaleta.model.Insumo;
import com.lacaleta.model.Producto;
import com.lacaleta.repository.CategoriaRepository;
import com.lacaleta.repository.InsumoRepository;
import com.lacaleta.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Override
    public void run(String... args) throws Exception {
        // Sembrar Categorías si está vacío
        if (categoriaRepository.count() == 0) {
            Categoria clasicas = new Categoria(null, "clasicas", "Pizzas Clásicas", "Recetas tradicionales horneadas a la leña", "bi-pie-chart");
            Categoria especiales = new Categoria(null, "especiales", "Pizzas Especiales & Fusión", "Ingredientes de autor y recetas marinas", "bi-fire");
            Categoria combos = new Categoria(null, "combos", "Combos & Promociones", "Pizzas con complementos", "bi-tag");
            Categoria acompanamientos = new Categoria(null, "acompanamientos", "Acompañamientos", "Para picar antes", "bi-basket");
            Categoria bebidas = new Categoria(null, "bebidas", "Bebidas", "Refrescantes", "bi-cup-straw");
            Categoria promociones = new Categoria(null, "promociones", "Promociones", "Ofertas especiales", "bi-star");

            categoriaRepository.saveAll(List.of(clasicas, especiales, combos, acompanamientos, bebidas, promociones));

            // Sembrar Insumos Iniciales si está vacío
            if (insumoRepository.count() == 0) {
                Insumo i1 = new Insumo(null, "INS-01", "Queso Mozzarella", 4.0, "kg", 5.0);
                Insumo i2 = new Insumo(null, "INS-02", "Salsa Pomodoro", 12.0, "Litros", 5.0);
                Insumo i3 = new Insumo(null, "INS-03", "Pepperoni Laminado", 7.0, "kg", 4.0);
                Insumo i4 = new Insumo(null, "INS-04", "Masa Artesanal", 25.0, "unidades", 10.0);
                insumoRepository.saveAll(List.of(i1, i2, i3, i4));
            }

            // Sembrar Productos Iniciales si está vacío
            if (productoRepository.count() == 0) {
                // Principales
                productoRepository.save(new Producto(null, "La Cómplice", "Nuestra pizza más leal con abundante pepperoni crocante", 36.00, "/images/producto_pepperoni.jpg", clasicas, "Familiar", true, 20));
                productoRepository.save(new Producto(null, "El Pacto", "Tocino ahumado crocante y jamón bajo un pacto de queso fundido", 32.00, "/images/producto_americana.jpg", clasicas, "Familiar", true, 25));
                productoRepository.save(new Producto(null, "La Coartada", "Dulzura de piña caramelizada con jamón selecto en secreto", 35.00, "/images/producto_hawaiana.jpg", clasicas, "Familiar", true, 18));
                productoRepository.save(new Producto(null, "La Confesión", "Pimientos asados, champiñones, aceitunas y albahaca fresca", 35.00, "/images/producto_vegetariana.jpg", clasicas, "Familiar", true, 15));
                productoRepository.save(new Producto(null, "La Cofradía", "Alianza contundente de pepperoni, carne sazonada y tocino", 38.00, "/images/producto_meat.jpg", clasicas, "Familiar", true, 22));

                // Combos
                productoRepository.save(new Producto(null, "Dúo Clandestino", "2 Pizzas Grandes mas Gaseosa de 1.5L", 54.90, "/images/producto_combo1.jpg", combos, "Combo", true, 15));
                productoRepository.save(new Producto(null, "Trío en Secreto", "3 Pizzas Medianas para compartir con el grupo selecto", 69.90, "/images/producto_combo2.jpg", combos, "Combo", true, 12));
                productoRepository.save(new Producto(null, "El Silencio", "1 Pizza Familiar a eleccion mas Palitos de Ajo artesanales", 44.90, "/images/producto_combo3.jpg", combos, "Combo", true, 20));
                productoRepository.save(new Producto(null, "Pacto Caleta", "2 Pizzas Familiares mas Alitas BBQ mas Gaseosa de 1.5L", 79.90, "/images/producto_combo4.jpg", combos, "Combo", true, 10));

                // Acompañamientos
                productoRepository.save(new Producto(null, "Palitos de Ajo", "Masa horneada con mantequilla de ajo, oregano y queso parmesano", 12.00, "/images/producto_combo3.jpg", acompanamientos, "Porción", true, 30));
                productoRepository.save(new Producto(null, "Alitas BBQ", "6 alitas de pollo banadas en salsa barbacoa ahumada artesanal", 18.00, "/images/producto_combo4.jpg", acompanamientos, "6 Unid", true, 25));
                productoRepository.save(new Producto(null, "Tequeños de Queso", "8 tequenos crocantes rellenos de queso con salsa guacamole secreta", 15.00, "/images/producto_meat.jpg", acompanamientos, "8 Unid", true, 20));

                // Bebidas
                productoRepository.save(new Producto(null, "Inca Kola 1.5L", "Botella no retornable bien helada para acompanar tu pizza", 9.00, "/images/producto_combo1.jpg", bebidas, "1.5L", true, 50));
                productoRepository.save(new Producto(null, "Coca Cola 1.5L", "Sabor original helada en presentacion familiar no retornable", 9.00, "/images/producto_combo1.jpg", bebidas, "1.5L", true, 50));
                productoRepository.save(new Producto(null, "Chicha Morada 1L", "Preparada con maiz morado, pina, manzana y toques de canela y clavo", 10.00, "/images/producto_combo2.jpg", bebidas, "1L", true, 30));

                // Promociones
                // 1 Persona
                productoRepository.save(new Producto(null, "La Cómplice Personal", "Nuestra pizza leal personal con abundante pepperoni crocante", 19.90, "/images/producto_pepperoni.jpg", promociones, "Personal", true, 30));
                productoRepository.save(new Producto(null, "El Pacto Personal", "Tocino crocante y jamón selecto bajo queso fundido personal", 17.90, "/images/producto_americana.jpg", promociones, "Personal", true, 30));
                productoRepository.save(new Producto(null, "La Coartada Personal", "Dulzura de piña caramelizada con jamón en porción individual", 18.90, "/images/producto_hawaiana.jpg", promociones, "Personal", true, 20));
                
                // Duo
                productoRepository.save(new Producto(null, "Dúo Cómplice", "2 Pizzas Medianas de Pepperoni crocante mas 2 Bebidas personales", 48.90, "/images/producto_combo1.jpg", promociones, "Dúo", true, 15));
                productoRepository.save(new Producto(null, "Dúo Pacto Clásico", "2 Pizzas Medianas para compartir en pareja", 46.90, "/images/producto_americana.jpg", promociones, "Dúo", true, 15));

                // Trio
                productoRepository.save(new Producto(null, "Trilogía Caleta", "3 Pizzas Medianas clasicas a eleccion para el grupo selecto", 68.90, "/images/producto_combo2.jpg", promociones, "Trío", true, 12));
                
                // Familiar
                productoRepository.save(new Producto(null, "Banquete Clandestino", "2 Pizzas Familiares mas 1 Porcion de Alitas BBQ mas Gaseosa 1.5L", 84.90, "/images/producto_combo4.jpg", promociones, "Familiar", true, 10));
            }
        }
    }
}
