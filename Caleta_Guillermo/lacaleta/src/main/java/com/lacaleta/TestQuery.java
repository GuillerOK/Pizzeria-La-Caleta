package com.lacaleta;

import com.lacaleta.repository.CategoriaRepository;
import com.lacaleta.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Profile("demo")
public class TestQuery implements CommandLineRunner {
    private final CategoriaRepository catRepo;
    private final ProductoRepository prodRepo;
    
    public TestQuery(CategoriaRepository catRepo, ProductoRepository prodRepo) {
        this.catRepo = catRepo;
        this.prodRepo = prodRepo;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== CATEGORIAS ===");
        catRepo.findAll().forEach(c -> System.out.println(c.getId() + ": " + c.getNombre() + " (codigo: " + c.getCodigo() + ")"));
        
        System.out.println("=== PRODUCTOS ===");
        prodRepo.findAll().forEach(p -> {
            String cat = p.getCategoria() != null ? p.getCategoria().getCodigo() : "NULL";
            System.out.println(p.getId() + ": " + p.getNombre() + " - Cat: " + cat);
        });
    }
}
