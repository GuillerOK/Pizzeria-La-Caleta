function mostrarSeccion(idSeccion, elementoBtn) {
    document.querySelectorAll('.caleta-seccion').forEach(sec => sec.classList.remove('activa'));
    document.querySelectorAll('.caleta-menu-btn').forEach(btn => btn.classList.remove('activo'));

    const seccionDestino = document.getElementById(idSeccion);
    if (seccionDestino) {
        seccionDestino.classList.add('activa');
    }
    if (elementoBtn) {
        elementoBtn.classList.add('activo');
    }

    const titulos = {
        'dashboard': 'Dashboard de Control',
        'productos': 'Catálogo de Pizzas & Menú',
        'stock': 'Control de Insumos y Materia Prima',
        'ventas': 'Historial de Pedidos',
        'clientes': 'Directorio de Clientes'
    };
    document.getElementById('tituloSeccion').innerText = titulos[idSeccion] || 'Panel de Administración';
}

function filtrarTabla(inputId, tablaId) {
    const input = document.getElementById(inputId);
    const filtro = input.value.toLowerCase();
    const tabla = document.getElementById(tablaId);
    const filas = tabla.getElementsByTagName('tr');

    for (let i = 1; i < filas.length; i++) {
        const filaTexto = filas[i].textContent.toLowerCase();
        filas[i].style.display = filaTexto.includes(filtro) ? '' : 'none';
    }
}

function abrirModalEditar(btn) {
    if (!btn) return;
    const editId = document.getElementById('edit-id');
    const editNombre = document.getElementById('edit-nombre');
    const editPrecio = document.getElementById('edit-precio');
    const editImg = document.getElementById('edit-img');
    const editTamano = document.getElementById('edit-tamano');
    const editCategoria = document.getElementById('edit-categoria');
    const editDisponible = document.getElementById('edit-disponible');

    if (editId) editId.value = btn.getAttribute('data-id') || '';
    if (editNombre) editNombre.value = btn.getAttribute('data-nombre') || '';
    if (editPrecio) editPrecio.value = btn.getAttribute('data-precio') || '';
    if (editImg) editImg.value = btn.getAttribute('data-img') || '';

    if (editTamano) {
        editTamano.value = btn.getAttribute('data-tamano') || 'Familiar';
    }
    if (editCategoria) {
        editCategoria.value = btn.getAttribute('data-categoria') || 'clasicas';
    }
    if (editDisponible) {
        const disp = btn.getAttribute('data-disponible');
        editDisponible.value = (disp === 'false') ? 'false' : 'true';
    }

    const modalEl = document.getElementById('modalEditarPizza');
    if (modalEl) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }
}

function abrirModalEditarInsumo(btn) {
    if (!btn) return;
    const editId = document.getElementById('edit-insumo-id');
    const editNombre = document.getElementById('edit-insumo-nombre');
    const editStock = document.getElementById('edit-insumo-stock');
    const editUnidad = document.getElementById('edit-insumo-unidad');
    const editMinimo = document.getElementById('edit-insumo-minimo');

    if (editId) editId.value = btn.getAttribute('data-id') || '';
    if (editNombre) editNombre.value = btn.getAttribute('data-nombre') || '';
    if (editStock) editStock.value = btn.getAttribute('data-stock') || '';
    if (editUnidad) editUnidad.value = btn.getAttribute('data-unidad') || '';
    if (editMinimo) editMinimo.value = btn.getAttribute('data-minimo') || '';

    const modalEl = document.getElementById('modalEditarInsumo');
    if (modalEl) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }
}



function cerrarSesion() {
    localStorage.removeItem("sesion_activa");
    localStorage.removeItem("token");
    window.location.href = "/logout";
}

window.addEventListener('DOMContentLoaded', () => {
    fetchDashboardMetrics();

    // Permite conservar la pestaña activa si hay hash en la URL (ej. #stock)
    if (window.location.hash) {
        const hash = window.location.hash.substring(1);
        const botonesNav = document.querySelectorAll('.caleta-menu-btn');
        if (hash === 'stock') {
            mostrarSeccion('stock', botonesNav[2]);
        } else if (hash === 'productos') {
            mostrarSeccion('productos', botonesNav[1]);
        }
    }
});

async function fetchDashboardMetrics() {
    try {
        const response = await fetch('/api/admin/dashboard/metricas');
        if (response.ok) {
            const data = await response.json();
            
            const kpiVentas = document.getElementById('kpi-ventas-dia');
            if (kpiVentas) kpiVentas.textContent = `S/ ${data.ventasDelDia.toFixed(2)}`;

            const kpiPizzas = document.getElementById('kpi-pizzas-catalogo');
            if (kpiPizzas) kpiPizzas.textContent = `${data.pizzasCatalogo} Variedades`;

            const kpiInsumos = document.getElementById('kpi-insumos-criticos');
            if (kpiInsumos) kpiInsumos.textContent = `${data.insumosCriticos} Alertas`;

            const kpiClientes = document.getElementById('kpi-clientes');
            if (kpiClientes) kpiClientes.textContent = `${data.clientesRegistrados}`;
        }
    } catch (error) {
        console.error('Error fetching dashboard metrics:', error);
    }
}