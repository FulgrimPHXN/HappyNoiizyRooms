package org.diceordieass.rooms.advice;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLock_returnsConflict() {
        var problem = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("Room", "id"));
        assertThat(problem.getStatus()).isEqualTo(409);
    }

    @Test
    void integrityViolation_doesNotExposeDatabaseDetails() {
        var problem = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("private_table_constraint"));
        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).doesNotContain("private_table_constraint");
    }
}
