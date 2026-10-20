package com.lacaleta.controller;

import com.lacaleta.model.Usuario;
import com.lacaleta.service.CategoriaService;
import com.lacaleta.service.CuponService;
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

    @Autowired
    private CuponService cuponService;

    @Autowired
    private com.lacaleta.repository.UsuarioRepository usuarioRepository;

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
    public String detalles(Model model, HttpSession session,
                           @RequestParam(defaultValue = "") String nombre) {
        cargarUsuarioEnModelo(model, session);
        model.addAttribute("variantes", productoService.listar().stream()
                .filter(p -> p.getNombre().equals(nombre) && Boolean.TRUE.equals(p.getDisponible())).toList());
        model.addAttribute("acompanamientos", productoService.obtenerAcompanamientos());
        model.addAttribute("bebidas", productoService.obtenerBebidas());
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

    @GetMapping({ "/forgotpassword", "/forgot-password" })
    public String forgotPassword() {
        return "account/forgotpassword";
    }

    @GetMapping({ "/verifycode", "/verify-code" })
    public String verifyCode() {
        return "account/verifycode";
    }

    @GetMapping({ "/resetpassword", "/reset-password" })
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
        model.addAttribute("insumosReceta", insumoService.listar());
        model.addAttribute("totalPizzas", productoService.listar().size());
        model.addAttribute("categorias", categoriaService.listar());

        var listaPedidos = pedidoRepository.findAllByOrderByFechaDesc();
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
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/login";
        cargarUsuarioEnModelo(model, session);
        return "checkout/shipping";
    }

    @GetMapping("/pago")
    public String pago(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/login";
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

    @GetMapping("/perfil")
    public String perfil(@RequestParam(value = "tab", required = false) String tab,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model,
                         HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");

        // Si el usuario no ha iniciado sesión, lo redirige al login
        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        // Refrescar usuario desde la BD
        var cuenta = usuarioService.buscarPorId(usuarioLogueado.getId());
        if (cuenta.isEmpty() || !Boolean.TRUE.equals(cuenta.get().getVerificado())) {
            session.invalidate();
            return "redirect:/login";
        }
        Usuario usuarioActualizado = cuenta.get();
        session.setAttribute("usuarioLogueado", usuarioActualizado);

        cargarUsuarioEnModelo(model, session);

        // Obtener historial de pedidos del usuario conectado
        var paginaPedidos = pedidoRepository.findByUsuarioId(usuarioActualizado.getId(),
                org.springframework.data.domain.PageRequest.of(Math.max(0, pagina), 20,
                        org.springframework.data.domain.Sort.by("fecha", "id").descending()));
        var misPedidos = paginaPedidos.getContent();

        double totalGastado = pedidoRepository.totalPorUsuario(usuarioActualizado.getId());
        int puntosCaleta = cuponService.calcularPuntosDisponibles(usuarioActualizado, totalGastado);
        if (usuarioActualizado.getPuntos() == null || !usuarioActualizado.getPuntos().equals(puntosCaleta)) {
            usuarioActualizado.setPuntos(puntosCaleta);
            usuarioRepository.save(usuarioActualizado);
        }

        var ultimoPedido = pedidoRepository.findFirstByUsuarioIdOrderByFechaDesc(usuarioActualizado.getId()).orElse(null);
        var misCupones = cuponService.listarPorUsuario(usuarioActualizado);
        var cuponesActivos = cuponService.listarActivos(usuarioActualizado);
        var catalogo = cuponService.obtenerCatalogo();

        model.addAttribute("pedidos", misPedidos);
        model.addAttribute("totalPedidos", paginaPedidos.getTotalElements());
        model.addAttribute("paginaPedidos", paginaPedidos);
        model.addAttribute("totalGastado", totalGastado);
        model.addAttribute("puntosCaleta", puntosCaleta);
        model.addAttribute("ultimoPedido", ultimoPedido);
        model.addAttribute("misCupones", misCupones);
        model.addAttribute("cuponesActivos", cuponesActivos);
        model.addAttribute("catalogoCupones", catalogo);
        model.addAttribute("tabActiva", (tab != null && !tab.isBlank()) ? tab : "resumen");

        return "account/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@RequestParam("nombre") String nombre,
                                   @RequestParam("telefono") String telefono,
                                   @RequestParam(value = "dni", required = false) String dni,
                                   HttpSession session,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        String nomClean = (nombre != null) ? nombre.trim() : "";
        String telClean = (telefono != null) ? telefono.trim() : "";
        String dniClean = (dni != null) ? dni.trim() : "";

        // Validaciones para público peruano
        if (!NAME_PATTERN.matcher(nomClean).matches()) {
            redirectAttributes.addFlashAttribute("error", "El nombre completo debe contener solo letras (entre 2 y 50 caracteres).");
            return "redirect:/perfil?tab=datos";
        }

        if (!PHONE_PERU_PATTERN.matcher(telClean).matches()) {
            redirectAttributes.addFlashAttribute("error", "El número de teléfono celular debe tener 9 dígitos y empezar con 9 (formato peruano).");
            return "redirect:/perfil?tab=datos";
        }

        if (!dniClean.isEmpty()) {
            if (!DNI_PATTERN.matcher(dniClean).matches()) {
                redirectAttributes.addFlashAttribute("error", "El DNI debe contener exactamente 8 dígitos numéricos.");
                return "redirect:/perfil?tab=datos";
            }
            usuarioLogueado.setDni(dniClean);
        } else {
            usuarioLogueado.setDni(null);
        }

        usuarioLogueado.setNombre(nomClean);
        usuarioLogueado.setTelefono(telClean);

        boolean actualizado = usuarioService.guardar(usuarioLogueado);
        if (actualizado) {
            session.setAttribute("usuarioLogueado", usuarioLogueado);
            redirectAttributes.addFlashAttribute("exito", "¡Tus datos personales se actualizaron correctamente!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al intentar actualizar tus datos.");
        }

        return "redirect:/perfil?tab=datos";
    }

    @PostMapping("/perfil/canjear-cupon")
    public String canjearCupon(@RequestParam("tipoCupon") String tipoCupon,
                               HttpSession session,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        double totalGastado = pedidoRepository.totalPorUsuario(usuarioLogueado.getId());

        var cuponCanjeado = cuponService.canjearCupon(usuarioLogueado, tipoCupon, totalGastado);
        if (cuponCanjeado != null) {
            redirectAttributes.addFlashAttribute("exito", "¡Felicitaciones! Canjeaste el '" + cuponCanjeado.getTitulo() + "' con éxito. Tu código promocional es: " + cuponCanjeado.getCodigo());
            return "redirect:/perfil?tab=cupones";
        } else {
            redirectAttributes.addFlashAttribute("error", "No cuentas con suficientes Puntos Caleta o el cupón seleccionado no es válido.");
            return "redirect:/perfil?tab=canjear";
        }
    }

    @PostMapping("/perfil/eliminar")
    public String eliminarPerfil(HttpSession session,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        try {
            usuarioService.eliminarCuenta(usuarioLogueado.getId());

            // 4. Invalidar sesión
            session.invalidate();

            redirectAttributes.addFlashAttribute("mensaje", "Tu cuenta ha sido eliminada permanentemente. Esperamos volver a verte pronto en La Caleta.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al intentar eliminar la cuenta. Vuelve a intentarlo.");
            return "redirect:/perfil";
        }
    }

    @PostMapping("/admin/clientes/eliminar/{id}")
    public String eliminarCliente(@org.springframework.web.bind.annotation.PathVariable("id") Integer id,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes flash,
                                  jakarta.servlet.http.HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null || !usuarioLogueado.esAdmin()) {
            return "redirect:/login";
        }

        try {
            if (usuarioRepository.existsById(id)) {
                usuarioRepository.deleteById(id);
                flash.addFlashAttribute("exito", "Cliente eliminado correctamente de la base de datos.");
            } else {
                flash.addFlashAttribute("error", "No se encontró el cliente que deseas eliminar.");
            }
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo eliminar el cliente. Puede tener pedidos asociados en la base de datos.");
        }
        return "redirect:/admin#clientes";
    }
}
