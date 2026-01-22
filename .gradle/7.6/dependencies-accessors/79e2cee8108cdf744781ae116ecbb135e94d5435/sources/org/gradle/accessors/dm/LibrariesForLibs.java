package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
*/
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects) {
        super(config, providers, objects);
    }

        /**
         * Creates a dependency provider for jacksonDatabind (com.fasterxml.jackson.core:jackson-databind)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJacksonDatabind() { return create("jacksonDatabind"); }

        /**
         * Creates a dependency provider for springBootActuator (org.springframework.boot:spring-boot-starter-actuator)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringBootActuator() { return create("springBootActuator"); }

        /**
         * Creates a dependency provider for springBootTest (org.springframework.boot:spring-boot-starter-test)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringBootTest() { return create("springBootTest"); }

        /**
         * Creates a dependency provider for springBootValidation (org.springframework.boot:spring-boot-starter-validation)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringBootValidation() { return create("springBootValidation"); }

        /**
         * Creates a dependency provider for springBootWeb (org.springframework.boot:spring-boot-starter-web)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringBootWeb() { return create("springBootWeb"); }

        /**
         * Creates a dependency provider for springKafka (org.springframework.kafka:spring-kafka)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringKafka() { return create("springKafka"); }

        /**
         * Creates a dependency provider for springKafkaTest (org.springframework.kafka:spring-kafka-test)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringKafkaTest() { return create("springKafkaTest"); }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() { return vaccForVersionAccessors; }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() { return baccForBundleAccessors; }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() { return paccForPluginAccessors; }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: java (17)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getJava() { return getVersion("java"); }

            /**
             * Returns the version associated to this alias: springBoot (3.4.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getSpringBoot() { return getVersion("springBoot"); }

            /**
             * Returns the version associated to this alias: springDepMgmt (1.1.6)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getSpringDepMgmt() { return getVersion("springDepMgmt"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config) { super(objects, providers, config); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a plugin provider for springBoot to the plugin id 'org.springframework.boot'
             * This plugin was declared in catalog libs.versions.toml
             */
            public Provider<PluginDependency> getSpringBoot() { return createPlugin("springBoot"); }

            /**
             * Creates a plugin provider for springDepMgmt to the plugin id 'io.spring.dependency-management'
             * This plugin was declared in catalog libs.versions.toml
             */
            public Provider<PluginDependency> getSpringDepMgmt() { return createPlugin("springDepMgmt"); }

    }

}
