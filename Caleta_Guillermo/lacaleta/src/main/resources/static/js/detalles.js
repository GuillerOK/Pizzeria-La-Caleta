let pasoActual = 1;
let precioBaseProducto = 36.00;

document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const nombre = params.get("nombre");
    const precio = params.get("precio");
    const img = params.get("img");
    const desc = params.get("desc");
    const tipo = params.get("tipo");
    const indiceEdicion = params.get("editar");

    if (nombre) {
        const elementoNombre = document.getElementById("detalleNombre");
        const breadcrumb = document.getElementById("breadcrumbNombre");
        if (elementoNombre) elementoNombre.innerText = nombre;
        if (breadcrumb) breadcrumb.innerText = nombre;
        document.title = `La Caleta | ${nombre}`;
    }

    if (desc) {
        const elementoDesc = document.getElementById("detalleDescripcion");
        if (elementoDesc) elementoDesc.innerText = desc;
    }

    if (tipo) {
        const badgeCat = document.getElementById("badgeCategoria");
        const tituloPers = document.getElementById("tituloPersonalizar");
        if (badgeCat) badgeCat.innerText = tipo;
        if (tituloPers) tituloPers.innerText = tipo.toLowerCase() === "combo" ? "Personaliza tu combo" : "Personaliza tu pizza";
    }

    if (img) {
        const elementoImg = document.getElementById("detalleImagen");
        if (elementoImg) elementoImg.src = img;
    }

    if (precio) {
        precioBaseProducto = parseFloat(precio);
        const radioMediana = document.getElementById("tam-mediana");
        const radioFamiliar = document.getElementById("tam-familiar");
        const radioMega = document.getElementById("tam-mega");
        if (radioMediana) radioMediana.value = precioBaseProducto.toFixed(2);
        if (radioFamiliar) radioFamiliar.value = (precioBaseProducto + 10.00).toFixed(2);
        if (radioMega) radioMega.value = (precioBaseProducto + 20.00).toFixed(2);
    }

    if (indiceEdicion !== null) {
        const carrito = JSON.parse(localStorage.getItem('carrito_caleta')) || [];
        const itemAEditar = carrito[parseInt(indiceEdicion)];

        if (itemAEditar) {
            const btnFinalizar = document.getElementById("btnFinalizar");
            if (btnFinalizar) {
                btnFinalizar.innerHTML = 'Guardar cambios <i class="fa-solid fa-check ms-1"></i>';
            }

            document.querySelectorAll('input[type="radio"]').forEach(radio => {
                const label = radio.dataset.label || radio.value;
                if (label && itemAEditar.nombre.includes(label)) {
                    radio.checked = true;
                    // Actualiza el resumen colapsable
                    const seccion = radio.name; // tamano, masa, cortesia
                    const textoResumen = document.getElementById(`texto-resumen-${seccion}`);
                    if (textoResumen) textoResumen.innerText = label;

                    const badge = document.getElementById(`badge-${seccion}`);
                    if (badge) {
                        badge.innerText = "Completado";
                        badge.className = "badge-completado";
                    }

                    const cuerpo = document.getElementById(`cuerpo-${seccion}`);
                    const box = document.getElementById(`box-resumen-${seccion}`);
                    if (cuerpo) cuerpo.classList.add("d-none");
                    if (box) box.classList.remove("d-none");
                }
            });
        }
    }
});

function alternarSeccion(idCuerpo, idResumenBox) {
    const cuerpo = document.getElementById(idCuerpo);
    const box = document.getElementById(idResumenBox);
    if (!cuerpo) return;

    if (cuerpo.classList.contains("d-none")) {
        cuerpo.classList.remove("d-none");
        if (box) box.classList.add("d-none");
    } else {
        if (cuerpo.querySelector('input[type="radio"]:checked')) {
            cuerpo.classList.add("d-none");
            if (box) box.classList.remove("d-none");
        }
    }
}

function seleccionarYContraer(tipo, inputRadio, idCuerpoActual, idResumenActual, idAlertaActual, idCuerpoSiguiente, idResumenSiguiente) {
    const textoResumen = document.getElementById(`texto-resumen-${tipo}`);
    if (textoResumen) textoResumen.innerText = inputRadio.dataset.label || inputRadio.value;

    const badgeActual = document.getElementById(`badge-${tipo}`);
    if (badgeActual) {
        badgeActual.innerText = "Completado";
        badgeActual.className = "badge-completado";
    }

    const alertaActual = document.getElementById(idAlertaActual);
    if (alertaActual) alertaActual.classList.add("d-none");

    const cuerpoActual = document.getElementById(idCuerpoActual);
    const boxActual = document.getElementById(idResumenActual);
    if (cuerpoActual) cuerpoActual.classList.add("d-none");
    if (boxActual) boxActual.classList.remove("d-none");

    if (idCuerpoSiguiente) {
        const cuerpoSig = document.getElementById(idCuerpoSiguiente);
        const boxSig = document.getElementById(idResumenSiguiente);
        if (cuerpoSig) cuerpoSig.classList.remove("d-none");
        if (boxSig) boxSig.classList.add("d-none");

        if (cuerpoSig && !cuerpoSig.querySelector('input[type="radio"]:checked')) {
            const alertaSig = document.getElementById(idCuerpoSiguiente.replace("cuerpo-", "alerta-"));
            if (alertaSig) alertaSig.classList.remove("d-none");
        }
    }
}

function irAlPaso(paso) {
    if (paso === 2) {
        const tamano = document.querySelector('input[name="tamano"]:checked');
        const masa = document.querySelector('input[name="masa"]:checked');
        const cortesia = document.querySelector('input[name="cortesia"]:checked');

        if (!tamano || !masa || !cortesia) {
            return;
        }
    }

    pasoActual = paso;
    document.getElementById("paso1-pizza").classList.toggle("d-none", paso !== 1);
    document.getElementById("paso2-extras").classList.toggle("d-none", paso !== 2);
    document.getElementById("btnAtras").classList.toggle("d-none", paso === 1);
    document.getElementById("btnSiguiente").classList.toggle("d-none", paso === 2);
    document.getElementById("btnFinalizar").classList.toggle("d-none", paso !== 2);

    const stepCircle2 = document.getElementById("stepCircle2");
    const stepLabel2 = document.getElementById("stepLabel2");
    const stepLine = document.getElementById("stepLine");

    if (paso === 1) {
        stepCircle2.className = "rounded-circle d-flex align-items-center justify-content-center fw-bold border border-secondary text-secondary bg-transparent";
        stepCircle2.innerText = "2";
        stepLabel2.className = "small fw-bold mt-1 text-secondary";
        stepLine.style.backgroundColor = "rgba(255,255,255,0.2)";
    } else {
        stepCircle2.className = "rounded-circle d-flex align-items-center justify-content-center fw-bold bg-ro text-white shadow-sm";
        stepCircle2.innerHTML = '<i class="fa-solid fa-check"></i>';
        stepLabel2.className = "small fw-bold mt-1 text-ro";
        stepLine.style.backgroundColor = "var(--color-rojo-naranja)";
    }
}


function enviarAlCarritoDesdeDetalle() {
    const elementoNombre = document.getElementById("detalleNombre");
    const elementoImagen = document.getElementById("detalleImagen");
    const elementoDesc = document.getElementById("detalleDescripcion");
    if (!elementoNombre || !elementoImagen) return;

    const nombreBase = elementoNombre.innerText.trim();
    const imagenUrl = elementoImagen.getAttribute("src");
    const descripcion = elementoDesc ? elementoDesc.innerText.trim() : "";

    const inputTamano = document.querySelector('input[name="tamano"]:checked');
    const labelTamano = inputTamano ? (inputTamano.dataset.label || "Mediana") : "Mediana";

    const inputMasa = document.querySelector('input[name="masa"]:checked');
    const labelMasa = inputMasa ? inputMasa.value : "Masa Tradicional";

    const inputCortesia = document.querySelector('input[name="cortesia"]:checked');
    const cortesiaSeleccionada = inputCortesia ? inputCortesia.value : '';

    let detalleProductoFinal = `${nombreBase} (${labelTamano}, ${labelMasa})`;
    if (cortesiaSeleccionada) {
        detalleProductoFinal += ` [Cortesía: ${cortesiaSeleccionada}]`;
    }

    const precioPizza = inputTamano && !isNaN(parseFloat(inputTamano.value)) ? parseFloat(inputTamano.value) : precioBaseProducto;

    const params = new URLSearchParams(window.location.search);
    const indiceEdicion = params.get("editar");

    if (indiceEdicion !== null) {
        const carrito = JSON.parse(localStorage.getItem('carrito_caleta')) || [];
        const idx = parseInt(indiceEdicion);

        if (carrito[idx]) {
            carrito[idx].productoId = Number(inputTamano.dataset.productoId);
            carrito[idx].nombre = detalleProductoFinal;
            carrito[idx].precio = precioPizza;
            carrito[idx].imagenUrl = imagenUrl;
            carrito[idx].descripcion = descripcion;
            localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
        }

        document.querySelectorAll('.item-extra-producto:checked').forEach(extra => {
            agregarAlCarrito(extra.dataset.nombre, parseFloat(extra.value), extra.dataset.img || "/images/banner_promocional.jpg", "", Number(extra.dataset.productoId));
        });

        setTimeout(() => {
            window.location.href = "/carrito";
        }, 300);
    } else {
        agregarAlCarrito(detalleProductoFinal, precioPizza, imagenUrl, descripcion, Number(inputTamano.dataset.productoId));

        document.querySelectorAll('.item-extra-producto:checked').forEach(extra => {
            agregarAlCarrito(extra.dataset.nombre, parseFloat(extra.value), extra.dataset.img || "/images/banner_promocional.jpg", "", Number(extra.dataset.productoId));
        });

        setTimeout(() => {
            window.location.href = "/menu";
        }, 1000);
    }
}