package service;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClearServiceTest {
    @Test
    public void testClearWorks() {
        var service = new ClearService(new MemoryDataAccess());
        assertDoesNotThrow(service::clear);
    }
}