package com.fortytwotalents.fotogallery.verification;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;

class TaikaiVerificationTest {

	@Test
	void shouldFulfillConstraints() {
		Taikai.builder()
			.namespace("com.fortytwotalents.fotogallery")
			.java(java -> java.noUsageOfDeprecatedAPIs()
				.methodsShouldNotDeclareGenericExceptions()
				.utilityClassesShouldBeFinalAndHavePrivateConstructor()
				.imports(imports -> imports.shouldHaveNoCycles().shouldNotImport("..internal.."))
				.naming(naming -> naming.classesShouldNotMatch(".*Impl")
					.methodsShouldNotMatch("^(foo$|bar$).*")
					.fieldsShouldNotMatch(".*(List|Set|Map)$")
					.constantsShouldFollowConventions()
					.interfacesShouldNotHavePrefixI()))
			.logging(logging -> logging.loggersShouldFollowConventions(Logger.class, "LOGGER", List.of(PRIVATE, FINAL)))
			.test(test -> test.junit(
					junit -> junit.classesShouldNotBeAnnotatedWithDisabled().methodsShouldNotBeAnnotatedWithDisabled()))
			.spring(spring -> spring.noAutowiredFields()
				.boot(boot -> boot.applicationClassShouldResideInPackage("com.fortytwotalents.fotogallery"))
				.configurations(configuration -> configuration.namesShouldEndWithConfiguration())
				.controllers(controllers -> controllers.shouldBeAnnotatedWithRestController()
					.namesShouldEndWithController()
					.shouldNotDependOnOtherControllers()
					.shouldBePackagePrivate())
				.services(services -> services.shouldBeAnnotatedWithService()
					.shouldNotDependOnControllers()
					.namesShouldEndWithService())
				.repositories(repositories -> repositories.shouldBeAnnotatedWithRepository()
					.shouldNotDependOnServices()
					.namesShouldEndWithRepository()))
			.build()
			.checkAll();
	}

}
