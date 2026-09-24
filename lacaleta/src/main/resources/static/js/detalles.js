// ==========================================
// CONFIGURADOR TIPO WIZARD (PIZZA -> EXTRAS)
// ==========================================

let pasoActual = 1;
let cantidadDetalleActual = 1;
let precioBaseProducto = 36.00;

document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const nombre = params.get("nombre");
    const precio = params.get("precio");
    const img = params.get("img");
    const desc = params.get("desc");
    const tipo = params.get("tipo");

    if (nombre) {
        const elementoNombre = document.getElementById("detalleNombre");
        if (elementoNombre) elementoNombre.innerText = nombre;
        document.title = `La Caleta | ${nombre}`;
    }

    if (desc) {
        const elementoDesc = document.getElementById("detalleDescripcion");
        if (elementoDesc) elementoDesc.innerText = desc;
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

        if (radioMediana) radioMediana.value = precioBaseProducto;
        if (radioFamiliar) radioFamiliar.value = (precioBaseProducto + 10.00).toFixed(2);
        if (radioMega) radioMega.value = (precioBaseProducto + 20.00).toFixed(2);
    }

    // Botones del contador
    const btnMenos = document.getElementById("btnMenos");
    const btnMas = document.getElementById("btnMas");

    if (btnMenos) {
        btnMenos.addEventListener("click", (e) => {
            e.preventDefault();
            ajustarContador(-1);
        });
    }

    if (btnMas) {
        btnMas.addEventListener("click", (e) => {
            e.preventDefault();
            ajustarContador(1);
        });
    }

    // Recalcular precio en tiempo real
    document.querySelectorAll('input[name="tamano"], .item-adicional').forEach(input => {
        input.addEventListener('change', actualizarPrecioTotal);
    });

    actualizarPrecioTotal();
});

// Control del Wizard / Pasos
function irAlPaso(paso) {
    pasoActual = paso;

    const paso1 = document.getElementById("paso1-pizza");
    const paso2 = document.getElementById("paso2-extras");
    const btnAtras = document.getElementById("btnAtras");
    const btnSiguiente = document.getElementById("btnSiguiente");
    const btnFinalizar = document.getElementById("btnFinalizar");

    // Stepper visual
    const stepCircle2 = document.getElementById("stepCircle2");
    const stepLabel2 = document.getElementById("stepLabel2");
    const stepLine = document.getElementById("stepLine");

    if (paso === 1) {
        paso1.classList.remove("d-none");
        paso2.classList.add("d-none");
        btnAtras.classList.add("d-none");
        btnSiguiente.classList.remove("d-none");
        btnFinalizar.classList.add("d-none");

        stepCircle2.className = "rounded-circle d-flex align-items-center justify-content-center fw-bold border border-secondary text-secondary bg-black";
        stepCircle2.innerHTML = "2";
        stepLabel2.className = "small fw-bold mt-1 text-secondary";
        stepLine.style.backgroundColor = "rgba(255,255,255,0.2)";
    } else if (paso === 2) {
        paso1.classList.add("d-none");
        paso2.classList.remove("d-none");
        btnAtras.classList.remove("d-none");
        btnSiguiente.classList.add("d-none");
        btnFinalizar.classList.remove("d-none");

        stepCircle2.className = "rounded-circle d-flex align-items-center justify-content-center fw-bold bg-ro text-white shadow-sm";
        stepCircle2.innerHTML = '<i class="fa-solid fa-check"></i>';
        stepLabel2.className = "small fw-bold mt-1 text-ro";
        stepLine.style.backgroundColor = "var(--color-rojo-naranja)";
    }

    actualizarPrecioTotal();
}

function ajustarContador(delta) {
    cantidadDetalleActual += delta;
    if (cantidadDetalleActual < 1) cantidadDetalleActual = 1;
    const contadorDOM = document.getElementById("contadorCantidad");
    if (contadorDOM) contadorDOM.innerText = cantidadDetalleActual;
    actualizarPrecioTotal();
}

function calcularPrecioUnitarioTotal() {
    let unitario = precioBaseProducto;

    const tamanoInput = document.querySelector('input[name="tamano"]:checked');
    if (tamanoInput) unitario = parseFloat(tamanoInput.value);

    // Sumar todos los adicionales seleccionados (ingredientes + acompañamientos/bebidas)
    document.querySelectorAll('.item-adicional:checked').forEach(item => {
        unitario += parseFloat(item.value);
    });

    return unitario;
}

function actualizarPrecioTotal() {
    const unitario = calcularPrecioUnitarioTotal();
    const total = (unitario * cantidadDetalleActual).toFixed(2);

    const txtSig = document.getElementById("precioTotalBoton");
    const txtFin = document.getElementById("precioTotalFinalBoton");

    if (txtSig) txtSig.innerText = `S/ ${total}`;
    if (txtFin) txtFin.innerText = `S/ ${total}`;
}

function enviarAlCarritoDesdeDetalle() {
    const elementoNombre = document.getElementById("detalleNombre");
    const elementoImagen = document.getElementById("detalleImagen");

    if (!elementoNombre || !elementoImagen) return;

    const nombreBase = elementoNombre.innerText.trim();
    const imagenUrl = elementoImagen.getAttribute("src");

    // 1. Tamaño y Masa
    const inputTamano = document.querySelector('input[name="tamano"]:checked');
    const labelTamano = inputTamano ? inputTamano.nextElementSibling.querySelector(".fw-bold").innerText.trim() : "Mediana";

    const inputMasa = document.querySelector('input[name="masa"]:checked');
    const labelMasa = inputMasa ? inputMasa.value : "Masa Tradicional";

    // 2. Toppings del Paso 1
    const toppings = [];
    document.querySelectorAll('#paso1-pizza .item-adicional:checked').forEach(item => {
        toppings.push(item.dataset.nombre);
    });

    let nombrePizzaFinal = `${nombreBase} (${labelTamano}, ${labelMasa})`;
    if (toppings.length > 0) {
        nombrePizzaFinal += ` + ${toppings.join(', ')}`;
    }

    // Precio unitario solo de la pizza configurada
    let precioPizza = inputTamano ? parseFloat(inputTamano.value) : precioBaseProducto;
    document.querySelectorAll('#paso1-pizza .item-adicional:checked').forEach(item => {
        precioPizza += parseFloat(item.value);
    });

    // Agregar la pizza configurada al carrito
    for (let i = 0; i < cantidadDetalleActual; i++) {
        agregarAlCarrito(nombrePizzaFinal, precioPizza, imagenUrl);
    }

    // 3. Agregar los Extras del Paso 2 como ítems independientes
    const extrasSeleccionados = [
        { id: "addLasagna", nombre: "Lasagna Boloñesa Tradicional", precio: 22.00, img: "/images/producto_meat.jpg" },
        { id: "addIncaKola", nombre: "Inca Kola 1.5L", precio: 9.00, img: "/images/producto_combo1.jpg" },
        { id: "addCocaCola", nombre: "Coca Cola 1.5L", precio: 9.00, img: "/images/producto_combo1.jpg" },
        { id: "addPalitos", nombre: "Palitos de Ajo Artesanales", precio: 12.00, img: "/images/producto_combo3.jpg" }
    ];

    extrasSeleccionados.forEach(extra => {
        const checkbox = document.getElementById(extra.id);
        if (checkbox && checkbox.checked) {
            agregarAlCarrito(extra.nombre, extra.precio, extra.img);
        }
    });

    // Opcional: volver al catálogo después de comprar
    setTimeout(() => {
        window.location.href = "/menu";
    }, 1200);
}