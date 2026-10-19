// Cambia el ejemplo y la ayuda del campo de documento según se elija cédula o pasaporte
document.addEventListener("DOMContentLoaded", function () {
    const formats = {
        CI: {placeholder: "1.234.567-8", hint: "Cédula uruguaya, con o sin puntos y guion."},
        PASSPORT: {placeholder: "AB1234567", hint: "Pasaporte de cualquier país: entre 6 y 12 letras o números."}
    };

    document.querySelectorAll("select[data-doc-input]").forEach(function (select) {
        const fieldId = select.dataset.docInput;
        const input = document.getElementById(fieldId);
        const hint = document.querySelector('[data-doc-hint="' + fieldId + '"]');

        function update() {
            const format = formats[select.value] || formats.CI;
            if (input) {
                input.placeholder = format.placeholder;
            }
            if (hint) {
                hint.textContent = format.hint;
            }
        }

        select.addEventListener("change", update);
        update();
    });
});
