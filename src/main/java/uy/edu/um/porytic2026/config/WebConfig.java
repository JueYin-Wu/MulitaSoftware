package uy.edu.um.porytic2026.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

// Hace que las imágenes subidas (carpeta "uploads") se puedan ver en /uploads/...
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.folder:uploads}")
    private String uploadsFolder;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadsFolder).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
