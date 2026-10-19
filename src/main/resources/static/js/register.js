// Muestra los campos de persona física o jurídica según el tipo elegido
document.addEventListener("DOMContentLoaded", function () {
    const radios = document.querySelectorAll('input[name="providerType"]');
    const sections = document.querySelectorAll("[data-type]");

    function update() {
        const checked = document.querySelector('input[name="providerType"]:checked');
        const type = checked ? checked.value : "FISIC";
        sections.forEach(function (section) {
            section.hidden = section.dataset.type !== type;
        });
    }

    radios.forEach(function (radio) {
        radio.addEventListener("change", update);
    });
    update();
});
