package uy.edu.um.porytic2026.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

// Guarda en disco las imágenes que suben los usuarios (por ahora, las fotos de perfil de los prestadores).
// Quedan en la carpeta "uploads" del proyecto y se sirven en /uploads/** (ver WebConfig).
@Service
public class FileStorageService {

    // Máximo por foto (las fotos de celular suelen pesar entre 3 y 8 MB)
    public static final int MAX_MB = 10;
    private static final long MAX_SIZE = MAX_MB * 1024L * 1024L;

    // Tipos de imagen aceptados y la extensión con la que se guardan
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path rootFolder;

    public FileStorageService(@Value("${app.uploads.folder:uploads}") String folder) {
        this.rootFolder = Paths.get(folder).toAbsolutePath().normalize();
    }

    // Guarda la imagen en uploads/<subfolder>/ y devuelve la ruta para usar en la página
    public String storeImage(MultipartFile file, String subfolder, String namePrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Elegí una imagen");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("La imagen tiene que ser JPG, PNG o WEBP");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("La imagen no puede pesar más de " + MAX_MB + " MB");
        }

        // Nombre al azar para que no se pisen archivos ni se pueda adivinar la ruta
        String fileName = namePrefix + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path folder = rootFolder.resolve(subfolder);
        try (InputStream input = file.getInputStream()) {
            Files.createDirectories(folder);
            Files.copy(input, folder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        return "/uploads/" + subfolder + "/" + fileName;
    }

    // Borra una imagen guardada antes (si existe)
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        Path file = rootFolder.resolve(url.substring("/uploads/".length())).normalize();
        if (!file.startsWith(rootFolder)) {
            return; // por seguridad, nunca se borra algo fuera de la carpeta uploads
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // si no se puede borrar, no es grave: solo queda el archivo viejo en disco
        }
    }
}
