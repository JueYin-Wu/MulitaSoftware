// Marca en verde las reglas de la contraseña a medida que el usuario escribe
document.addEventListener("DOMContentLoaded", function () {
    const box = document.getElementById("password-rules");
    const password = document.getElementById("password");
    const confirm = document.getElementById("confirmPassword");
    if (!box || !password || !confirm) {
        return;
    }

    function update() {
        const rules = {
            length: password.value.length >= 8,
            upper: /\p{Lu}/u.test(password.value),
            lower: /\p{Ll}/u.test(password.value),
            symbol: /[^\p{L}\p{N}\s]/u.test(password.value),
            match: password.value.length > 0 && password.value === confirm.value
        };
        box.querySelectorAll("li[data-rule]").forEach(function (item) {
            item.classList.toggle("ok", rules[item.dataset.rule]);
        });
    }

    password.addEventListener("input", update);
    confirm.addEventListener("input", update);
    update();
});
