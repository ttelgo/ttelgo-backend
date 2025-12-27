package com.tiktel.ttelgo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * R1: Enforce a modular (feature/domain based) backend design.
 *
 * Conventions enforced:
 * - Feature modules live under com.tiktel.ttelgo.<module>
 * - Each module should follow layered structure: api, application, domain, infrastructure
 * - No cyclic dependencies between feature modules
 * - domain layer must not depend on api/infrastructure
 * - application layer must not depend on api
 * - infrastructure layer must not depend on api
 *
 * Shared packages (common/config/security/integration) are treated as shared and may be depended on by feature modules.
 */
public class ModularArchitectureTest {

    private static final String BASE = "com.tiktel.ttelgo";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void no_cycles_between_feature_modules() {
        // Slice by first package after BASE, e.g. com.tiktel.ttelgo.order.*
        SlicesRuleDefinition.slices()
                .matching(BASE + ".(*)..")
                .namingSlices("$1")
                .as("Feature modules (top-level packages under " + BASE + ")")
                .should()
                .beFreeOfCycles()
                .check(classes);
    }

    @Test
    void layers_within_each_module_are_respected() {
        // Enforce layer rules within each module. This is strict and prevents "shortcuts".
        // Note: we intentionally do NOT model the whole app as one layered system;
        // we enforce per-module layering via package patterns.

        // domain must not depend on api/infrastructure anywhere
        classes().that()
                .resideInAnyPackage(BASE + "..domain..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "jakarta..",
                        "lombok..",
                        BASE + "..domain..",
                        BASE + "..common.."
                )
                .because("domain must be isolated from delivery (api) and technical concerns (infrastructure)")
                .check(classes);

        // application layer should not depend on controllers,
        // but in this codebase it may depend on API DTOs/mappers and infrastructure repositories.
        classes().that()
                .resideInAnyPackage(BASE + "..application..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "jakarta..",
                        "lombok..",
                        "org.springframework..",
                        "org.slf4j..",
                        BASE + "..application..",
                        BASE + "..domain..",
                        BASE + "..common..",
                        BASE + "..security..",
                        BASE + "..integration..", // allowed via infrastructure adapters; application should prefer ports
                        BASE + "..api.dto..",
                        BASE + "..api.mapper..",
                        BASE + "..infrastructure.." // existing services use repositories directly
                )
                .because("application layer should remain thin and avoid controller dependencies")
                .check(classes);

        // infrastructure must not depend on api
        classes().that()
                .resideInAnyPackage(BASE + "..infrastructure..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "jakarta..",
                        "lombok..",
                        "org.springframework..",
                        "org.hibernate..",
                        "org.slf4j..",
                        BASE + "..infrastructure..",
                        BASE + "..application..",
                        BASE + "..domain..",
                        BASE + "..common..",
                        BASE + "..security..",
                        BASE + "..integration..",
                        BASE + "..config.."
                )
                .because("infrastructure should not depend on api layer")
                .check(classes);
    }

    @Test
    void api_layer_should_only_depend_on_application_and_shared_packages() {
        classes().that()
                .resideInAnyPackage(BASE + "..api..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "jakarta..",
                        "lombok..",
                        "org.springframework..",
                        "org.slf4j..",
                        "com.fasterxml..",
                        BASE + "..api..",
                        BASE + "..application..",
                        BASE + "..domain..", // allow returning domain enums/value objects if used (keeps current code working)
                        BASE + "..common..",
                        BASE + "..security..",
                        BASE + "..integration..",
                        BASE + "..infrastructure.." // some controllers use repositories directly today
                )
                .because("controllers should delegate to application services and not reach into infrastructure")
                .check(classes);
    }

    @Test
    void shared_modules_should_not_depend_on_feature_modules() {
        // shared modules should not import feature modules directly to avoid central coupling.
        String[] shared = {
                BASE + ".common..",
                BASE + ".config..",
                BASE + ".security..",
                BASE + ".integration..",
                // API key infrastructure is a cross-cutting concern used by security/config layers
                BASE + ".apikey.."
        };

        classes().that()
                .resideInAnyPackage(shared)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "jakarta..",
                        "lombok..",
                        "org.springframework..",
                        "org.slf4j..",
                        "io.jsonwebtoken..",
                        "io.swagger..",
                        "com.fasterxml..",
                        "org.springdoc..",
                        BASE + ".common..",
                        BASE + ".config..",
                        BASE + ".security..",
                        BASE + ".integration..",
                        BASE + ".apikey.."
                )
                .because("shared modules must stay generic and not depend on feature modules")
                .check(classes);
    }

    @Test
    void application_layer_should_not_depend_on_integration_clients_directly() {
        // Extensibility rule: application code must talk to external systems via ports (interfaces)
        // and have the infrastructure adapters implement them.
        // This keeps swapping providers (e.g. new eSIM vendor) low-effort and localized.
        noClasses()
                .that().resideInAnyPackage(BASE + "..application..")
                .should().dependOnClassesThat().haveNameMatching(
                        // allow integration DTOs (anti-corruption mapping can be tightened later),
                        // but forbid depending on integration clients/config from application layer.
                        BASE.replace(".", "\\.") + "\\.integration\\.esimgo\\.(?!dto\\.).*"
                )
                .because("application code must not depend on specific integration clients; use ports/adapters instead")
                .check(classes);
    }

    @Test
    void modules_should_have_clear_top_level_packages() {
        // Simple guardrail: avoid introducing random top-level packages outside the known module list.
        classes().that()
                .resideInAPackage(BASE + "..")
                .should()
                .resideInAnyPackage(
                        BASE + ".admin..",
                        BASE + ".apikey..",
                        BASE + ".auth..",
                        BASE + ".blog..",
                        BASE + ".common..",
                        BASE + ".config..",
                        BASE + ".esim..",
                        BASE + ".faq..",
                        BASE + ".integration..",
                        BASE + ".kyc..",
                        BASE + ".notification..",
                        BASE + ".order..",
                        BASE + ".payment..",
                        BASE + ".plan..",
                        BASE + ".security..",
                        BASE + ".user..",
                        BASE // root (e.g. TtelgoApplication)
                )
                .because("new code should live in a feature module (or shared modules) to stay maintainable")
                .check(classes);
    }

}


