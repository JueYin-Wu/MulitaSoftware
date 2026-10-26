package uy.edu.um.porytic2026.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.porytic2026.api.dto.ApiDtos.ActivityResponse;
import uy.edu.um.porytic2026.api.dto.ApiDtos.DailyActivityResponse;
import uy.edu.um.porytic2026.api.dto.ApiDtos.TopActivityResponse;
import uy.edu.um.porytic2026.services.PublicApiService;

import java.util.List;

// Recurso "activities".
//   GET /api/v1/activities?date=2026-11-07&department=rocha[&locality=la-paloma] -> actividades de un día (200 / 400)
//   GET /api/v1/activities/top?department=rocha[&month=2026-10]                  -> top 3 del mes         (200 / 400)
//   GET /api/v1/activities/search?q=kayak                                         -> búsqueda              (200 / 400)
// Si no hay resultados se devuelve 200 con una lista vacía: el pedido estuvo bien, solo no hay datos.
@RestController
@RequestMapping("/api/v1/activities")
public class ApiActivityController {

    private final PublicApiService apiService;

    public ApiActivityController(PublicApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    public ResponseEntity<List<DailyActivityResponse>> byDay(@RequestParam(required = false) String date,
                                                             @RequestParam(required = false) String department,
                                                             @RequestParam(required = false) String locality) {
        return ResponseEntity.ok(apiService.activitiesOnDay(date, department, locality));
    }

    @GetMapping("/top")
    public ResponseEntity<List<TopActivityResponse>> top(@RequestParam(required = false) String department,
                                                         @RequestParam(required = false) String month) {
        return ResponseEntity.ok(apiService.topRated(department, month));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ActivityResponse>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(apiService.search(q));
    }
}
