package uy.edu.um.porytic2026.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Si alguien sube un archivo más grande que el máximo, vuelve a la página anterior con un aviso en vez de una página de error
@ControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fileTooLarge(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "La imagen no puede pesar más de 2 MB");
        String previousPage = request.getHeader("Referer"); // la página donde estaba el formulario
        return "redirect:" + (previousPage != null ? previousPage : "/");
    }
}
