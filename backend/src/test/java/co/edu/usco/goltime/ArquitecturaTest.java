package co.edu.usco.goltime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** Verifica que la arquitectura hexagonal no se viole: dominio puro, dependencias hacia adentro. */
class ArquitecturaTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("co.edu.usco.goltime");

    @Test
    void elDominioNoDependeDeSpringNiDePersistencia() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "jakarta.servlet..")
                .allowEmptyShould(true)
                .check(CLASES);
    }

    @Test
    void elDominioNoDependeDeAplicacionNiInfraestructura() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage("..aplicacion..", "..infraestructura..")
                .allowEmptyShould(true)
                .check(CLASES);
    }

    @Test
    void laAplicacionNoDependeDeInfraestructura() {
        noClasses().that().resideInAPackage("..aplicacion..")
                .should().dependOnClassesThat().resideInAPackage("..infraestructura..")
                .allowEmptyShould(true)
                .check(CLASES);
    }

    @Test
    void losModulosNoTienenDependenciasCiclicas() {
        slices().matching("co.edu.usco.goltime.(*)..")
                .should().beFreeOfCycles()
                .check(CLASES);
    }
}
