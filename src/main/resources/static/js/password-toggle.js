// Agrega un botón con forma de ojo a cada campo de contraseña para mostrar u ocultar lo que se escribe.
// Funciona en cualquier página que incluya este archivo (login, registros y nueva contraseña).
document.addEventListener("DOMContentLoaded", function () {
    const eyeOpen = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" ' +
        'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/>' +
        '<circle cx="12" cy="12" r="3"/></svg>';
    const eyeClosed = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" ' +
        'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-11-7-11-7' +
        'a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19"/>' +
        '<path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

    document.querySelectorAll('input[type="password"]').forEach(function (input) {
        // Se envuelve el campo para poder ubicar el botón adentro, a la derecha
        const wrapper = document.createElement("div");
        wrapper.className = "password-field";
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);

        const button = document.createElement("button");
        button.type = "button"; // para que no envíe el formulario
        button.className = "password-toggle";
        wrapper.appendChild(button);

        function render(visible) {
            input.type = visible ? "text" : "password";
            button.innerHTML = visible ? eyeClosed : eyeOpen;
            button.setAttribute("aria-label", visible ? "Ocultar contraseña" : "Mostrar contraseña");
            button.setAttribute("title", visible ? "Ocultar contraseña" : "Mostrar contraseña");
            button.setAttribute("aria-pressed", visible ? "true" : "false");
        }

        button.addEventListener("click", function () {
            render(input.type === "password");
            input.focus();
        });
        render(false);
    });
});
