// Formulario de reseña: muestra qué significa la valoración elegida y cuenta los caracteres del comentario
document.addEventListener("DOMContentLoaded", function () {
    const labels = { 1: "Mala", 2: "Regular", 3: "Buena", 4: "Muy buena", 5: "¡Excelente!" };
    const text = document.getElementById("rating-text");
    document.querySelectorAll('.star-input input[name="rating"]').forEach(function (input) {
        input.addEventListener("change", function () {
            text.textContent = input.value + " de 5 · " + labels[input.value];
        });
        if (input.checked) {
            text.textContent = input.value + " de 5 · " + labels[input.value];
        }
    });

    const comment = document.getElementById("comment");
    const count = document.getElementById("comment-count");
    function updateCount() {
        count.textContent = comment.value.trim().length;
    }
    comment.addEventListener("input", updateCount);
    updateCount();
});
