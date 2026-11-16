package uy.edu.um.porytic2026.dto;

import java.util.ArrayList;
import java.util.List;

// Una página de una lista larga (por ejemplo, las actividades de a 30).
// Se usa con el fragmento fragments/pagination.html, que dibuja los botones de página.
// Las páginas se numeran desde 1, como las ve el usuario.
public class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    private PageResult(List<T> items, int page, int pageSize, int totalItems, int totalPages) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    // Toma de la lista completa solo los elementos de la página pedida.
    // Si la página no existe (por ejemplo ?page=99 o ?page=-1), se muestra la más cercana.
    public static <T> PageResult<T> of(List<T> all, int requestedPage, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("El tamaño de página tiene que ser al menos 1");
        }
        int total = all == null ? 0 : all.size();
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize); // división redondeando para arriba
        int page = Math.min(Math.max(requestedPage, 1), totalPages);
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<T> items = total == 0 ? List.of() : List.copyOf(all.subList(from, to));
        return new PageResult<>(items, page, pageSize, total, totalPages);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return page == 1;
    }

    public boolean isLast() {
        return page == totalPages;
    }

    // Hay que mostrar los botones solo si hay más de una página
    public boolean isPaginated() {
        return totalPages > 1;
    }

    // Número del primer y último elemento que se muestra (para "Mostrando 31 a 60 de 75")
    public int getFirstItemNumber() {
        return totalItems == 0 ? 0 : (page - 1) * pageSize + 1;
    }

    public int getLastItemNumber() {
        return Math.min(page * pageSize, totalItems);
    }

    // Números de página para los botones. Con muchas páginas no se muestran todas:
    // siempre la primera, la última y las dos vecinas de la actual; los huecos se marcan con 0 ("…").
    // Ejemplo con 20 páginas y la actual 10: [1, 0, 8, 9, 10, 11, 12, 0, 20]
    public List<Integer> getPageNumbers() {
        List<Integer> numbers = new ArrayList<>();
        int previous = 0;
        for (int n = 1; n <= totalPages; n++) {
            boolean show = n == 1 || n == totalPages || Math.abs(n - page) <= 2;
            if (!show) {
                continue;
            }
            if (n - previous > 1) {
                numbers.add(0); // hueco
            }
            numbers.add(n);
            previous = n;
        }
        return numbers;
    }
}
