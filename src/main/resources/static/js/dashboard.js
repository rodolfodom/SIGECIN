/*
 * Gráficas del dashboard con Chart.js. Los datos los incrusta el servidor en
 * window.SIGECIN_CHARTS al renderizar la página (no hay endpoints JSON).
 */
(function () {
    'use strict';

    const charts = window.SIGECIN_CHARTS || {};
    if (!window.Chart) {
        return;
    }

    const css = getComputedStyle(document.documentElement);
    const primary = css.getPropertyValue('--bs-primary').trim() || '#0d6efd';
    const text = css.getPropertyValue('--bs-secondary-color').trim() || '#6c757d';
    Chart.defaults.color = text;
    Chart.defaults.font.family = css.getPropertyValue('--bs-body-font-family').trim() || undefined;

    function barChart(canvasId, data, horizontal) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !data) {
            return;
        }
        new Chart(canvas, {
            type: 'bar',
            data: {
                labels: data.labels,
                datasets: [{label: data.label, data: data.data, backgroundColor: primary, borderRadius: 4, maxBarThickness: 48}]
            },
            options: {
                indexAxis: horizontal ? 'y' : 'x',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {legend: {display: false}},
                scales: {
                    // Las reservas son enteras
                    [horizontal ? 'x' : 'y']: {beginAtZero: true, ticks: {precision: 0}},
                    [horizontal ? 'y' : 'x']: {grid: {display: false}}
                }
            }
        });
    }

    barChart('weekly-chart', charts.weekly, false);
    barChart('services-chart', charts.services, true);
})();
