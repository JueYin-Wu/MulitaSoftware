// Formulario "Nueva actividad" del portal del prestador
document.addEventListener("DOMContentLoaded", function () {
    // Muestra u oculta los campos que dependen de un checkbox
    // Uso: <input type="checkbox" data-toggle="id-del-bloque"> ... <div id="id-del-bloque">
    document.querySelectorAll("input[data-toggle]").forEach(function (checkbox) {
        const target = document.getElementById(checkbox.dataset.toggle);
        if (!target) {
            return;
        }

        function update() {
            target.hidden = !checkbox.checked;
        }

        checkbox.addEventListener("change", update);
        update();
    });

    // Avisa enseguida si se eligen más fotos de las permitidas (el servidor lo vuelve a controlar)
    const photos = document.getElementById("photos");
    const photosError = document.getElementById("photos-error");
    if (photos && photosError) {
        const max = parseInt(photos.dataset.max, 10);
        photos.addEventListener("change", function () {
            if (photos.files.length > max) {
                photosError.textContent = "Podés subir hasta " + max + " fotos. Elegiste " + photos.files.length + ".";
                photosError.hidden = false;
                photos.classList.add("is-invalid");
                photos.value = "";
            } else {
                photosError.hidden = true;
                photos.classList.remove("is-invalid");
            }
        });
    }
});
