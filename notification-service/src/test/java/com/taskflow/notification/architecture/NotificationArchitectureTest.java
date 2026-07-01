package com.taskflow.notification.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class NotificationArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter().importPackages("com.taskflow.notification");
    }

    @Test
    void applicationLayer_doesNotDependOnControllerLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..controller..");

        rule.check(classes);
    }

    @Test
    void infrastructureLayer_doesNotDependOnControllerLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..controller..");

        rule.check(classes);
    }
}
