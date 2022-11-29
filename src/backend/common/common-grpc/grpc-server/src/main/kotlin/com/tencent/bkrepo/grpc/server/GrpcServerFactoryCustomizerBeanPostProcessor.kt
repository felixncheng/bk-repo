package com.tencent.bkrepo.grpc.server

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.util.LambdaSafe
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.util.Assert

class GrpcServerFactoryCustomizerBeanPostProcessor : BeanPostProcessor, BeanFactoryAware {
    private lateinit var beanFactory: ListableBeanFactory
    private var customizers: List<GrpcServerFactoryCustomizer>? = null
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (bean is GrpcServerFactory) {
            postProcessBeforeInitialization(bean)
        }
        return bean
    }

    override fun setBeanFactory(beanFactory: BeanFactory) {
        Assert.isInstanceOf(ListableBeanFactory::class.java, beanFactory)
        this.beanFactory = beanFactory as ListableBeanFactory
    }

    private fun postProcessBeforeInitialization(serverFactory: GrpcServerFactory) {
        LambdaSafe.callbacks(GrpcServerFactoryCustomizer::class.java, getCustomizers(), serverFactory)
            .withLogger(GrpcServerFactoryCustomizerBeanPostProcessor::class.java)
            .invoke { it.customize(serverFactory) }
    }

    private fun getCustomizers(): List<GrpcServerFactoryCustomizer> {
        customizers ?: let {
            customizers = getGrpcServerFactoryCustomizerBeans().toList()
            customizers?.sortedWith(AnnotationAwareOrderComparator.INSTANCE)
        }
        return customizers!!
    }

    private fun getGrpcServerFactoryCustomizerBeans(): Collection<GrpcServerFactoryCustomizer> {
        return this.beanFactory.getBeansOfType(GrpcServerFactoryCustomizer::class.java, false, false).values
    }
}
