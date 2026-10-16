// Desplazamiento horizontal de carruseles
function desplazar(id, desplazamiento) {
    const contenedor = document.getElementById(id);
    if (contenedor) {
        contenedor.scrollBy({ left: desplazamiento, behavior: 'smooth' });
    }
}

// Filtrado de pestañas de categorías
function cambiarSeccion(idSeccion, botonPresionado = null) {
    const secciones = document.querySelectorAll('.seccion-menu');
    secciones.forEach(sec => sec.classList.add('d-none'));

    const seccionActiva = document.getElementById(idSeccion);
    if (seccionActiva) {
        seccionActiva.classList.remove('d-none');
    }

    const botones = document.querySelectorAll('.btn-categoria');
    botones.forEach(btn => btn.classList.remove('active'));

    if (botonPresionado) {
        botonPresionado.classList.add('active');
    } else {
        const botonCoincidente = Array.from(botones).find(btn => 
            btn.getAttribute('onclick')?.includes(idSeccion)
        );
        if (botonCoincidente) {
            botonCoincidente.classList.add('active');
        }
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function procesarHash() {
    const hash = window.location.hash.replace('#', '');
    if (hash && document.getElementById(hash)) {
        cambiarSeccion(hash);
    }
}

window.addEventListener('DOMContentLoaded', procesarHash);
window.addEventListener('hashchange', procesarHash);