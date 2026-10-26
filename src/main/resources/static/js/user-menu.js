// Menú del usuario en el header: se cierra al hacer clic afuera o al apretar Escape
document.addEventListener("DOMContentLoaded", function () {
    const menu = document.querySelector(".user-menu");
    if (!menu) {
        return;
    }
    document.addEventListener("click", function (event) {
        if (menu.open && !menu.contains(event.target)) {
            menu.open = false;
        }
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && menu.open) {
            menu.open = false;
            menu.querySelector("summary").focus();
        }
    });
});
