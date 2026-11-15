package dojo;

import static dojo.CustomerSpec.active;
import static dojo.CustomerSpec.inCountry;
import static dojo.CustomerSpec.minOrders;
import static dojo.GradingSupport.code;
import static dojo.GradingSupport.rawSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CustomerSpecGradingTest {
    static final Customer ANA = new Customer("ana", "PT", true, 12);
    static final Customer BO = new Customer("bo", "PT", true, 0);
    static final Customer CY = new Customer("cy", "ES", true, 3);
    static final Customer DEE = new Customer("dee", "PT", false, 40);
    static final Customer EVA = new Customer("eva", "FR", false, 0);
    static final List<Customer> ALL = List.of(ANA, BO, CY, DEE, EVA);

    private final CustomerRepository repository = new CustomerRepository(ALL);

    @Test
    void the_old_finders_answer_as_before() {
        assertEquals(List.of(ANA, BO, CY), repository.findActive());
        assertEquals(List.of(CY), repository.findActiveInCountry("ES"));
        assertEquals(List.of(ANA), repository.findLoyalInCountry("PT", 1));
        assertEquals(List.of(BO, DEE, EVA), repository.findDormant());
        assertEquals(1, repository.countActiveOutside("PT"));
        assertEquals(3, repository.countActiveOutside("IT"));
    }

    @Test
    void specs_combine_without_new_finders() {
        assertEquals(List.of(ANA, CY, DEE), repository.findAll(minOrders(3)));
        assertEquals(List.of(DEE, EVA), repository.findAll(active().not()));
        assertEquals(List.of(ANA, CY, DEE), repository.findAll(inCountry("ES").or(minOrders(10))));
        assertEquals(2, repository.count(inCountry("PT").and(minOrders(10))));
        assertEquals(List.of(), repository.findAll(active().and(active().not())));
    }

    @Test
    void combinators_build_plain_records() {
        assertEquals(new And(new IsActive(), new InCountry("PT")), active().and(inCountry("PT")));
        assertEquals(new Or(new MinOrders(2), new Not(new IsActive())), minOrders(2).or(active().not()));
        assertTrue(new Not(new InCountry("FR")).isSatisfiedBy(ANA));
    }

    @Test
    void sql_uses_placeholders_and_keeps_parameter_order() {
        assertEquals(new SqlWhere("active = TRUE", List.of()), CustomerQueries.where(active()));
        assertEquals(new SqlWhere("(active = TRUE AND country = ?)", List.of("PT")),
                CustomerQueries.where(active().and(inCountry("PT"))));
        assertEquals(new SqlWhere("((active = TRUE AND country = ?) AND orders >= ?)", List.of("PT", 10)),
                CustomerQueries.where(active().and(inCountry("PT")).and(minOrders(10))));
        assertEquals(new SqlWhere("(NOT (active = TRUE) OR NOT (orders >= ?))", List.of(1)),
                CustomerQueries.where(active().not().or(minOrders(1).not())));
        assertEquals(new SqlWhere("NOT (country = ? OR orders >= ?)", List.of("ES", 5)),
                CustomerQueries.where(inCountry("ES").or(minOrders(5)).not()));
    }

    @Test
    void hostile_values_stay_out_of_the_sql_text() {
        String evil = "x' OR '1'='1";
        SqlWhere where = CustomerQueries.where(inCountry(evil));
        assertEquals("country = ?", where.clause());
        assertEquals(List.of(evil), where.params());
    }

    @Test
    void the_spec_is_a_sealed_family_of_records() {
        assertTrue(CustomerSpec.class.isSealed(), "CustomerSpec must be a sealed interface");
        Set<String> permitted = Arrays.stream(CustomerSpec.class.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("IsActive", "InCountry", "MinOrders", "And", "Or", "Not"), permitted);
        for (Class<?> type : CustomerSpec.class.getPermittedSubclasses()) {
            assertTrue(type.isRecord(), type.getSimpleName() + " should be a record");
        }
        assertTrue(SqlWhere.class.isRecord(), "SqlWhere should be a record");
    }

    @Test
    void conditions_live_only_in_the_specs() {
        String repository = code("dojo.CustomerRepository");
        for (String forbidden : List.of(".active()", ".country()", ".orders()", "for (", "if (")) {
            assertFalse(repository.contains(forbidden),
                    "CustomerRepository should express every finder as a spec; found " + forbidden);
        }
        for (String interpreter : List.of("dojo.CustomerSpec", "dojo.CustomerQueries")) {
            String code = code(interpreter);
            assertFalse(code.contains("instanceof"), interpreter + ": use a switch over the sealed specs");
            assertFalse(code.contains("default ->"), interpreter + ": a switch over sealed specs needs no default branch");
        }
        assertFalse(rawSource("dojo.CustomerQueries").contains("'"), "values must never be quoted into the SQL text");
    }
}
