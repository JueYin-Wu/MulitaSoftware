// Formulario de reserva: actualiza el total cuando cambia la cantidad de participantes
document.addEventListener("DOMContentLoaded", function () {
    const input = document.getElementById("participantCount");
    const total = document.getElementById("booking-total");
    if (!input || !total) {
        return;
    }
    const price = parseFloat(input.dataset.price);
    const formatter = new Intl.NumberFormat("es-UY", { maximumFractionDigits: 0 });

    function update() {
        const count = parseInt(input.value, 10);
        total.textContent = count > 0 ? "$ " + formatter.format(price * count) : "$ 0";
    }

    input.addEventListener("input", update);
});
