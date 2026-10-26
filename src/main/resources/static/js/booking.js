// Formulario de reserva:
//  - actualiza el total cuando cambia la cantidad de participantes
//  - muestra un bloque de datos por cada acompañante (cantidad - 1). Los bloques ocultos quedan
//    desactivados (disabled) para que no se manden al servidor.
document.addEventListener("DOMContentLoaded", function () {
    const input = document.getElementById("participantCount");
    const total = document.getElementById("booking-total");
    if (!input || !total) {
        return;
    }
    const price = parseFloat(input.dataset.price);
    const formatter = new Intl.NumberFormat("es-UY", { maximumFractionDigits: 0 });
    const companionsBox = document.getElementById("companions");
    const companions = document.querySelectorAll(".companion");

    function update() {
        const count = parseInt(input.value, 10);
        total.textContent = count > 0 ? "$ " + formatter.format(price * count) : "$ 0";

        const needed = count > 1 ? count - 1 : 0;
        companions.forEach(function (fieldset, index) {
            const show = index < needed;
            fieldset.hidden = !show;
            fieldset.disabled = !show;
        });
        if (companionsBox) {
            companionsBox.hidden = needed === 0;
        }
    }

    input.addEventListener("input", update);
});
