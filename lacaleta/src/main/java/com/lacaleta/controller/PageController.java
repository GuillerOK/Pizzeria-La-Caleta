package com.lacaleta.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("menu")
    public String menu() {
        return "shop/menu";
    }

    @GetMapping("/promociones")
    public String promociones() {
        return "shop/promociones";
    }

    @GetMapping("/locales")
    public String locales() {
        return "shop/locales";
    }

    @GetMapping("/detalles")
    public String detalles() {
        return "shop/detalles";
    }

    @GetMapping("/login")
    public String login() {
        return "account/login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "account/create";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "account/forgotpassword";
    }

    @GetMapping("/verify-code")
    public String verifyCode() {
        return "account/verifycode";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "account/resetpassword";
    }

    @GetMapping("/carrito")
    public String carrito() {
        return "checkout/cart";
    }

    @GetMapping("/pago")
    public String pago() {
        return "checkout/payment";
    }

    @GetMapping("/envio")
    public String envio() {
        return "checkout/shipping";
    }

    @GetMapping("/delivery")
    public String delivery() {
        return "info/delivery";
    }

    @GetMapping("/faq")
    public String faq() {
        return "info/faq";
    }

    @GetMapping("/terminos")
    public String terminos() {
        return "info/terminos";
    }

    @GetMapping("/administracion")
    public String admin() {
        return "admin/admin";
    }
}