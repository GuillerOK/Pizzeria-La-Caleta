function mostrarPaginaGraficos(num) {
    const p1 = document.getElementById("paginaGraficos1");
    const p2 = document.getElementById("paginaGraficos2");
    const t1 = document.getElementById("tab1-link");
    const t2 = document.getElementById("tab2-link");

    if (num === 1) {
        p1.style.display = "block";
        p2.style.display = "none";
        t1.classList.add("activo");
        t2.classList.remove("activo");
    } else {
        p1.style.display = "none";
        p2.style.display = "block";
        t1.classList.remove("activo");
        t2.classList.add("activo");
    }
}
window.addEventListener('DOMContentLoaded', () => {

    const colorNaranja = 'rgb(255, 69, 0)';
    const colorCarmesi = 'rgb(187, 20, 53)';

    // Configuración general de colores oscuros para ejes y leyendas en fondo claro
    const configTextoGrafico = {
        plugins: {
            legend: { labels: { color: '#374151', font: { weight: '600' } } }
        },
        scales: {
            x: { ticks: { color: '#4b5563' }, grid: { color: 'rgba(0, 0, 0, 0.05)' } },
            y: { ticks: { color: '#4b5563' }, grid: { color: 'rgba(0, 0, 0, 0.05)' } }
        }
    };

    async function cargarGraficos() {
        try {
            const response = await fetch('/api/admin/dashboard/metricas');
            if (!response.ok) return;
            const data = await response.json();

            // Valores por defecto en caso de no haber data
            const pizzasLabels = data.pizzasLabels && data.pizzasLabels.length > 0 ? data.pizzasLabels : ['Pepperoni', 'Hawaiana', 'Americana', 'Meat Lovers', 'Vegetariana'];
            const pizzasData = data.pizzasData && data.pizzasData.length > 0 ? data.pizzasData : [0, 0, 0, 0, 0];
            const ingresosSemanales = data.graficoIngresosSemanales || [0, 0, 0, 0];
            const graficoPagos = data.graficoPagos || [0, 0, 0];
            const insumosLabels = data.insumosLabels && data.insumosLabels.length > 0 ? data.insumosLabels : ['Mozzarella', 'Salsa Tomate', 'Pepperoni', 'Harina'];
            const insumosData = data.insumosData && data.insumosData.length > 0 ? data.insumosData : [0, 0, 0, 0];

            // 1. Barras: Pizzas
            new Chart(document.getElementById('chartBarrasPizzas').getContext('2d'), {
                type: 'bar',
                data: {
                    labels: pizzasLabels,
                    datasets: [{
                        label: 'Ventas en Soles (S/)',
                        data: pizzasData,
                        backgroundColor: [colorNaranja, colorCarmesi, '#d97706', '#dc2626', '#16a34a'],
                        borderRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    ...configTextoGrafico
                }
            });

            // 2. Lineal: Ingresos
            new Chart(document.getElementById('chartLinealMensual').getContext('2d'), {
                type: 'line',
                data: {
                    labels: ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'],
                    datasets: [{
                        label: 'Ingresos Semanales (S/)',
                        data: ingresosSemanales,
                        borderColor: colorNaranja,
                        backgroundColor: 'rgba(255, 69, 0, 0.15)',
                        fill: true,
                        tension: 0.35,
                        pointRadius: 5
                    }]
                },
                options: {
                    responsive: true,
                    ...configTextoGrafico
                }
            });

            // 3. Circular: Métodos de Pago
            new Chart(document.getElementById('chartCircularPagos').getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: ['Yape / Plin', 'Efectivo', 'Tarjeta Visa/Mastercard'],
                    datasets: [{
                        data: graficoPagos,
                        backgroundColor: [colorNaranja, colorCarmesi, '#64748b']
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { labels: { color: '#374151', font: { weight: '600' } } }
                    }
                }
            });

            // 4. Horizontal: Insumos
            new Chart(document.getElementById('chartStockInsumos').getContext('2d'), {
                type: 'bar',
                data: {
                    labels: insumosLabels,
                    datasets: [{
                        label: 'Unidades Disponibles',
                        data: insumosData,
                        backgroundColor: colorCarmesi,
                        borderRadius: 4
                    }]
                },
                options: {
                    indexAxis: 'y',
                    responsive: true,
                    ...configTextoGrafico
                }
            });
            
        } catch (error) {
            console.error("Error al cargar datos del dashboard: ", error);
        }
    }

    cargarGraficos();
});