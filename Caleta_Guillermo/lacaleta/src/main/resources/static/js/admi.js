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
    document.getElementById('edit-id').value = btn.getAttribute('data-id');
    document.getElementById('edit-nombre').value = btn.getAttribute('data-nombre');
    document.getElementById('edit-precio').value = btn.getAttribute('data-precio');
    document.getElementById('edit-img').value = btn.getAttribute('data-img');

    if (document.getElementById('edit-tamano')) {
        document.getElementById('edit-tamano').value = btn.getAttribute('data-tamano') || 'Familiar';
    }
    if (document.getElementById('edit-categoria')) {
        document.getElementById('edit-categoria').value = btn.getAttribute('data-categoria') || 'clasicas';
    }
    if (document.getElementById('edit-disponible')) {
        document.getElementById('edit-disponible').value = btn.getAttribute('data-disponible');
    }

    const modal = new bootstrap.Modal(document.getElementById('modalEditarPizza'));
    modal.show();
}

function abrirModalEditarInsumo(btn) {
    document.getElementById('edit-insumo-id').value = btn.getAttribute('data-id');
    document.getElementById('edit-insumo-nombre').value = btn.getAttribute('data-nombre');
    document.getElementById('edit-insumo-stock').value = btn.getAttribute('data-stock');
    document.getElementById('edit-insumo-unidad').value = btn.getAttribute('data-unidad');
    document.getElementById('edit-insumo-minimo').value = btn.getAttribute('data-minimo');

    const modal = new bootstrap.Modal(document.getElementById('modalEditarInsumo'));
    modal.show();
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