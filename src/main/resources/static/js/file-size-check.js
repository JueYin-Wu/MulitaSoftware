// Antes de subir fotos: si alguna pesa más de 10 MB (o no es JPG, PNG o WEBP), avisa en la página
// y no manda el formulario. Así el turista o el prestador no termina en una página de error.
document.addEventListener("DOMContentLoaded", function () {
    const MAX_BYTES = 10 * 1024 * 1024;
    const TYPES = ["image/jpeg", "image/png", "image/webp"];

    document.querySelectorAll('input[type="file"]').forEach(function (input) {
        const message = document.createElement("p");
        message.className = "field-error";
        message.hidden = true;
        input.insertAdjacentElement("afterend", message);

        function problem() {
            for (const file of input.files) {
                if (!TYPES.includes(file.type)) {
                    return "\"" + file.name + "\" no es una imagen JPG, PNG o WEBP.";
                }
                if (file.size > MAX_BYTES) {
                    const mb = (file.size / 1024 / 1024).toFixed(1).replace(".", ",");
                    return "\"" + file.name + "\" pesa " + mb + " MB. El máximo es 10 MB por foto.";
                }
            }
            return null;
        }

        input.addEventListener("change", function () {
            const text = problem();
            message.textContent = text || "";
            message.hidden = !text;
            input.classList.toggle("is-invalid", !!text);
        });

        if (input.form) {
            input.form.addEventListener("submit", function (event) {
                const text = problem();
                if (text) {
                    event.preventDefault();
                    message.textContent = text;
                    message.hidden = false;
                    input.focus();
                }
            });
        }
    });
});
