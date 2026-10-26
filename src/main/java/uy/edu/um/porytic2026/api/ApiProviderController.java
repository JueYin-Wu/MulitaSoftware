package uy.edu.um.porytic2026.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.porytic2026.api.dto.ApiDtos.ProviderResponse;
import uy.edu.um.porytic2026.api.dto.ApiDtos.VerificationResponse;
import uy.edu.um.porytic2026.services.PublicApiService;

import java.util.List;

// Recurso "providers" (emprendimientos). Todo en minúscula y con versión, como pide el estándar REST.
//   GET /api/v1/providers                            -> listado de emprendimientos          (200)
//   GET /api/v1/providers/{id}/verification          -> verificar un emprendimiento por id  (200 / 404)
//   GET /api/v1/providers/verification?rut=21456...  -> verificar un emprendimiento por RUT (200 / 400 / 404)
@RestController
@RequestMapping("/api/v1/providers")
public class ApiProviderController {

    private final PublicApiService apiService;

    public ApiProviderController(PublicApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> list() {
        return ResponseEntity.ok(apiService.listProviders());
    }

    @GetMapping("/{id}/verification")
    public ResponseEntity<VerificationResponse> verify(@PathVariable Long id) {
        return ResponseEntity.ok(apiService.verify(id));
    }

    @GetMapping("/verification")
    public ResponseEntity<VerificationResponse> verifyByRut(@RequestParam String rut) {
        return ResponseEntity.ok(apiService.verifyByTaxId(rut));
    }
}
