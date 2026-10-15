package com.lacaleta.controller;

import com.lacaleta.model.Usuario;
import com.lacaleta.service.CategoriaService;
import com.lacaleta.service.InsumoService;
import com.lacaleta.service.ProductoService;
import com.lacaleta.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import com.lacaleta.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
public class PageController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private InsumoService insumoService;

    @Autowired
    private UsuarioService usuarioService;

    // Patrones de validación Regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PERU_PATTERN = Pattern.compile("^9\\d{8}$");
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$");

    private void cargarUsuarioEnModelo(Model model, HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado != null) {
            model.addAttribute("usuario", usuarioLogueado);
        }
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        model.addAttribute("clasicas", productoService.obtenerClasicas());
        model.addAttribute("combos", productoService.obtenerCombos());
        cargarUsuarioEnModelo(model, session);
        return "index";
    }

    @GetMapping("/menu")
    public String menu(Model model, HttpSession session) {
        model.addAttribute("promociones", productoService.obtenerPromociones());
        model.addAttribute("principales", productoService.obtenerClasicas());
        model.addAttribute("combos", productoService.obtenerCombos());
        model.addAttribute("acompanamientos", productoService.obtenerAcompanamientos());
        model.addAttribute("bebidas", productoService.obtenerBebidas());
        model.addAttribute("categorias", categoriaService.listar());
        cargarUsuarioEnModelo(model, session);
        return "shop/menu";
    }

    @GetMapping("/promociones")
    public String promociones(Model model, HttpSession session) {
        model.addAttribute("promos1Persona", productoService.obtenerPromos1Persona());
        model.addAttribute("promosDuo", productoService.obtenerPromosDuo());
        model.addAttribute("promosTrio", productoService.obtenerPromosTrio());
        model.addAttribute("promosFamiliares", productoService.obtenerPromosFamiliares());
        cargarUsuarioEnModelo(model, session);
        return "shop/promociones";
    }

    @GetMapping("/detalles")
    public String detalles(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "shop/detalles";
    }

    @GetMapping("/locales")
    public String locales(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "shop/locales";
    }

    @GetMapping("/login")
    public String login() {
        return "account/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "account/create";
    }

    @GetMapping("/forgotpassword")
    public String forgotPassword() {
        return "account/forgotpassword";
    }

    @GetMapping("/verifycode")
    public String verifyCode() {
        return "account/verifycode";
    }

    @GetMapping("/resetpassword")
    public String resetPassword() {
        return "account/resetpassword";
    }

    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:/login";
        }
        if (!usuarioLogueado.esAdmin()) {
            return "redirect:/";
        }
        cargarUsuarioEnModelo(model, session);
        return "admin/reportes";
    }

    @GetMapping({ "/admin", "/administracion" })
    public String admin(
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "disponible", required = false) Boolean disponible,
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "buscarInsumo", required = false) String buscarInsumo,
            Model model,
            HttpSession session) {

        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:/login";
        }
        if (!usuarioLogueado.esAdmin()) {
            return "redirect:/";
        }

        cargarUsuarioEnModelo(model, session);

        var listaPizzas = productoService.listarConFiltros(categoria, disponible, buscar);
        var listaInsumos = insumoService.listarConFiltro(buscarInsumo);

        model.addAttribute("pizzas", listaPizzas);
        model.addAttribute("productos", listaPizzas);
        model.addAttribute("insumos", listaInsumos);
        model.addAttribute("totalPizzas", productoService.listar().size());
        model.addAttribute("categorias", categoriaService.listar());

        var listaPedidos = pedidoRepository.findAll();
        var listaClientes = usuarioService.listar().stream()
                .filter(u -> Integer.valueOf(1).equals(u.getRolId()))
                .toList();

        model.addAttribute("pedidos", listaPedidos);
        model.addAttribute("clientes", listaClientes);

        return "admin/admin";
    }

    @GetMapping({ "/carrito", "/cart" })
    public String carrito(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "checkout/cart";
    }

    @GetMapping("/envio")
    public String envio(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "checkout/shipping";
    }

    @GetMapping("/pago")
    public String pago(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "checkout/payment";
    }

    @GetMapping("/faq")
    public String faq(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "info/faq";
    }

    @GetMapping("/terminos")
    public String terminos(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "info/terminos";
    }

    @GetMapping("/delivery")
    public String delivery(Model model, HttpSession session) {
        cargarUsuarioEnModelo(model, session);
        return "info/delivery";
    }

}