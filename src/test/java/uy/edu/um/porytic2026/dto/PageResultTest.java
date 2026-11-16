package uy.edu.um.porytic2026.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

// Tests unitarios de PageResult (paginación de listas largas, por ejemplo las actividades de a 30)
class PageResultTest {

    // Lista con los números del 1 al n
    private static List<Integer> numbers(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    @Test
    void aShortListFitsInOnePageWithoutButtons() {
        PageResult<Integer> page = PageResult.of(numbers(12), 1, 30);

        assertEquals(12, page.getItems().size());
        assertEquals(1, page.getTotalPages());
        assertFalse(page.isPaginated());
    }

    @Test
    void exactlyThirtyItemsIsStillOnePage() {
        PageResult<Integer> page = PageResult.of(numbers(30), 1, 30);

        assertEquals(1, page.getTotalPages());
        assertFalse(page.isPaginated());
    }

    @Test
    void thirtyOneItemsMakeTwoPages() {
        PageResult<Integer> second = PageResult.of(numbers(31), 2, 30);

        assertEquals(2, second.getTotalPages());
        assertTrue(second.isPaginated());
        assertEquals(List.of(31), second.getItems());
        assertEquals(31, second.getFirstItemNumber());
        assertEquals(31, second.getLastItemNumber());
        assertTrue(second.isLast());
        assertFalse(second.isFirst());
    }

    @Test
    void eachPageHasItsOwnItems() {
        PageResult<Integer> page = PageResult.of(numbers(75), 2, 30);

        assertEquals(31, page.getItems().get(0));
        assertEquals(60, page.getItems().get(29));
        assertEquals(3, page.getTotalPages());
        assertEquals(75, page.getTotalItems());
    }

    @Test
    void aPageOutOfRangeShowsTheClosestOne() {
        assertEquals(3, PageResult.of(numbers(75), 99, 30).getPage());
        assertEquals(1, PageResult.of(numbers(75), 0, 30).getPage());
        assertEquals(1, PageResult.of(numbers(75), -5, 30).getPage());
    }

    @Test
    void anEmptyListHasOneEmptyPage() {
        PageResult<Integer> page = PageResult.of(List.of(), 1, 30);

        assertTrue(page.getItems().isEmpty());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getFirstItemNumber());
        assertFalse(page.isPaginated());
    }

    @Test
    void withManyPagesOnlyTheNeighboursAreShownAndGapsAreMarkedWithZero() {
        PageResult<Integer> page = PageResult.of(numbers(600), 10, 30); // 20 páginas

        assertEquals(List.of(1, 0, 8, 9, 10, 11, 12, 0, 20), page.getPageNumbers());
    }

    @Test
    void withFewPagesAllNumbersAreShown() {
        PageResult<Integer> page = PageResult.of(numbers(100), 1, 30); // 4 páginas

        assertEquals(List.of(1, 2, 3, 4), page.getPageNumbers());
    }
}
