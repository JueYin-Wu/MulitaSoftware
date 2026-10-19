// Carrusel de fotos de la página de una actividad: flechas, miniaturas y teclado
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-gallery]").forEach(function (gallery) {
        const slides = gallery.querySelectorAll(".gallery-slide");
        const thumbs = gallery.querySelectorAll(".gallery-thumb");
        const counter = gallery.querySelector("[data-gallery-counter]");
        let current = 0;

        function show(index) {
            current = (index + slides.length) % slides.length; // da la vuelta al llegar al final
            slides.forEach(function (slide, i) {
                slide.classList.toggle("active", i === current);
            });
            thumbs.forEach(function (thumb, i) {
                thumb.classList.toggle("active", i === current);
            });
            if (counter) {
                counter.textContent = (current + 1) + " / " + slides.length;
            }
        }

        const prev = gallery.querySelector("[data-gallery-prev]");
        const next = gallery.querySelector("[data-gallery-next]");
        if (prev) {
            prev.addEventListener("click", function () { show(current - 1); });
        }
        if (next) {
            next.addEventListener("click", function () { show(current + 1); });
        }
        thumbs.forEach(function (thumb) {
            thumb.addEventListener("click", function () { show(Number(thumb.dataset.index)); });
        });

        // Flechas del teclado cuando el carrusel tiene el foco
        gallery.setAttribute("tabindex", "0");
        gallery.addEventListener("keydown", function (event) {
            if (event.key === "ArrowLeft") { show(current - 1); }
            if (event.key === "ArrowRight") { show(current + 1); }
        });

        if (slides.length > 0) {
            show(0);
        }
    });
});
