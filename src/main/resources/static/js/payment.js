// Página de pago (todo pasa en nuestra página, sin ir a Mercado Pago):
//  1) Se dibuja el formulario de tarjeta de Mercado Pago (Card Payment Brick).
//  2) Al pagar, Mercado Pago convierte la tarjeta en un TOKEN de un solo uso y nosotros mandamos
//     solo ese token a nuestro servidor, que cobra con la API de Mercado Pago.
//  3) Se recarga la página para mostrar el resultado (aprobado, rechazado o en proceso).
//  Si el pago quedó "en proceso", cada 5 segundos se pregunta cómo terminó.
document.addEventListener("DOMContentLoaded", function () {
    const box = document.querySelector(".payment-box");
    if (!box) {
        return;
    }
    setUpCardForm(box.querySelector(".card-payment"));
    if (box.dataset.polling === "true") {
        startPolling(box.dataset.statusUrl);
    }
});

function setUpCardForm(container) {
    if (!container) {
        return;
    }
    const errorBox = document.getElementById("card-payment-error");
    const loading = document.getElementById("card-payment-loading");

    function showError(message) {
        errorBox.textContent = message;
        errorBox.hidden = false;
    }

    if (typeof MercadoPago === "undefined") {
        if (loading) loading.remove();
        showError("No se pudo cargar el formulario de pago. Revisá tu conexión y recargá la página.");
        return;
    }

    const mp = new MercadoPago(container.dataset.publicKey, { locale: "es-UY" });
    mp.bricks().create("cardPayment", "cardPaymentBrick_container", {
        initialization: {
            amount: Number(container.dataset.amount),
            payer: { email: container.dataset.email }
        },
        customization: {
            visual: { style: { theme: "dark" } },
            paymentMethods: { maxInstallments: 1 }
        },
        callbacks: {
            onReady: function () {
                if (loading) loading.remove();
            },
            onSubmit: function (formData) {
                errorBox.hidden = true;
                const payer = formData.payer || {};
                const id = payer.identification || {};
                const headers = { "Content-Type": "application/json", "Accept": "application/json" };
                headers[container.dataset.csrfHeader] = container.dataset.csrfToken;

                return fetch(container.dataset.payUrl, {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({
                        token: formData.token,
                        paymentMethodId: formData.payment_method_id,
                        issuerId: formData.issuer_id != null ? String(formData.issuer_id) : null,
                        installments: formData.installments,
                        identificationType: id.type || null,
                        identificationNumber: id.number || null
                    })
                }).then(function (response) {
                    return response.json().catch(function () { return {}; }).then(function (data) {
                        if (!response.ok) {
                            showError(data.message || "No se pudo procesar el pago. Probá de nuevo.");
                            throw new Error(data.message || "payment error");
                        }
                        window.location.reload(); // la página muestra el resultado
                    });
                });
            },
            onError: function (error) {
                console.error("Formulario de pago:", error);
                if (loading) loading.remove();
            }
        }
    });
}

function startPolling(url) {
    let attempts = 0;

    function check() {
        attempts++;
        fetch(url, { headers: { "Accept": "application/json" } })
            .then(function (response) { return response.ok ? response.json() : null; })
            .then(function (data) {
                if (data && (data.bookingStatus !== "PENDING" || data.paymentStatus !== "PENDING")) {
                    window.location.reload();
                    return;
                }
                if (attempts < 120) {           // deja de preguntar a los 10 minutos
                    setTimeout(check, 5000);
                }
            })
            .catch(function () {
                if (attempts < 120) {
                    setTimeout(check, 5000);
                }
            });
    }
    setTimeout(check, 5000);
}
