package com.tencent.bkrepo.grpc.server

import com.tencent.bkrepo.grpc.server.interceptor.InterceptorConfiguration
import java.util.function.Supplier
import kotlin.streams.toList
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.type.AnnotationMetadata
import org.springframework.util.ObjectUtils

@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(GrpcServerProperties::class)
@Import(
    GrpcServerAutoConfiguration.BeanPostProcessorsRegistrar::class,
    InterceptorConfiguration::class
)
class GrpcServerAutoConfiguration {

    @Bean
    fun grpcServerFactory(builderCustomizers: ObjectProvider<GrpcServerBuilderCustomizer>): GrpcServerFactory {
        val factory = GrpcServerFactory()
        factory.getBuilderCustomizers().addAll(builderCustomizers.orderedStream().toList())
        return factory
    }

    @Bean
    fun grpcServerFactoryCustomizer(serverProperties: GrpcServerProperties): GrpcServerFactoryCustomizer {
        return GrpcPropertiesServerFactoryCustomizer(serverProperties)
    }

    @Bean
    fun grpcServiceServerBuilderCustomizer(applicationContext: GenericApplicationContext): GrpcServiceServerBuilderCustomizer {
        return GrpcServiceServerBuilderCustomizer(applicationContext)
    }

    @Bean
    fun grpcGlobalInterceptorServerBuilderCustomizer(applicationContext: GenericApplicationContext):
        GrpcGlobalInterceptorServerBuilderCustomizer {
        return GrpcGlobalInterceptorServerBuilderCustomizer(applicationContext)
    }

    @Bean
    fun grpcApplicationContext(
        factory: GrpcServerFactory,
        applicationContext: GenericApplicationContext
    ): GrpcApplicationContext {
        val grpcApplicationContext = GrpcApplicationContext(applicationContext, factory)
        grpcApplicationContext.createGrpcServer()
        return grpcApplicationContext
    }

    class BeanPostProcessorsRegistrar : ImportBeanDefinitionRegistrar, BeanFactoryAware {
        private lateinit var beanFactory: ConfigurableListableBeanFactory
        override fun setBeanFactory(beanFactory: BeanFactory) {
            if (beanFactory is ConfigurableListableBeanFactory) {
                this.beanFactory = beanFactory
            }
        }

        override fun registerBeanDefinitions(
            importingClassMetadata: AnnotationMetadata,
            registry: BeanDefinitionRegistry,
            importBeanNameGenerator: BeanNameGenerator
        ) {
            registerSyntheticBeanIfMissing(
                registry,
                GrpcServerFactoryCustomizerBeanPostProcessor::class.java
            ) { GrpcServerFactoryCustomizerBeanPostProcessor() }
        }

        private fun <T> registerSyntheticBeanIfMissing(
            registry: BeanDefinitionRegistry,
            beanClass: Class<T>,
            instanceSupplier: Supplier<T>
        ) {
            if (ObjectUtils.isEmpty(beanFactory.getBeanNamesForType(beanClass, true, false))) {
                val beanDefinition = RootBeanDefinition(beanClass, instanceSupplier)
                beanDefinition.isSynthetic = true
                registry.registerBeanDefinition("grpcServerFactoryCustomizerBeanPostProcessor", beanDefinition)
            }
        }
    }
}
