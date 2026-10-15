function desplazarASeccion(idDestino, boton) {
    const destino = document.getElementById(idDestino);
    if (!destino) {
        console.warn("No se encontró el elemento con ID:", idDestino);
        return;
    }

    const header = document.getElementById('header');
    const navPromo = document.getElementById('categorias-promo');
    const offset = (header ? header.offsetHeight : 0) + (navPromo ? navPromo.offsetHeight : 0) + 15;

    const posicionElemento = destino.getBoundingClientRect().top + window.pageYOffset;
    const posicionFinal = posicionElemento - offset;

    window.scrollTo({
        top: posicionFinal,
        behavior: 'smooth'
    });

    document.querySelectorAll('#categorias-promo .btn-categoria').forEach(btn => btn.classList.remove('active'));
    if (boton) {
        boton.classList.add('active');
    }
}

document.addEventListener("DOMContentLoaded", () => {
    if (typeof verificarAccesoAdmin === "function") {
        verificarAccesoAdmin();
    }
});