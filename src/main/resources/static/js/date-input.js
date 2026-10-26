// Campos de fecha:
//  - al hacer clic en cualquier parte del campo se abre el calendario (no solo en el ícono)
//  - marca el campo vacío con la clase "is-empty" para mostrar "dd/mm/aaaa" en gris
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll('input[type="date"]').forEach(function (input) {
        function refresh() {
            input.classList.toggle("is-empty", !input.value);
        }
        input.addEventListener("click", function () {
            if (typeof input.showPicker === "function") {
                try {
                    input.showPicker();
                } catch (e) {
                    // algunos navegadores no permiten abrirlo así; queda el ícono
                }
            }
        });
        input.addEventListener("input", refresh);
        input.addEventListener("change", refresh);
        refresh();
    });
});
