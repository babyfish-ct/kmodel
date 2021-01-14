package org.babyfish.kmodel.spring.jdbc

import org.slf4j.LoggerFactory
import org.springframework.aop.TargetSource
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
import org.springframework.aop.support.DefaultIntroductionAdvisor
import org.springframework.context.ApplicationContext
import javax.sql.DataSource

internal class DataSourceAutoProxyCreator(
    applicationContext: ApplicationContext
) : AbstractAutoProxyCreator() {

    private val seataAutoProxyCreator: AbstractAutoProxyCreator?

    private val advisor = DefaultIntroductionAdvisor(DataSourceAutoProxyAdvice())

    init {
        seataAutoProxyCreator =
            try {
                Class.forName(SETA_AUTO_PROXY_CREATOR_CLASS_NAME)
            } catch (ex: ClassNotFoundException) {
                null
            }
            ?.let {
                applicationContext.getBean(it) as AbstractAutoProxyCreator?
            }
        if (seataAutoProxyCreator !== null) {
            LOGGER.info("kmodel-jdbc is used with seata")
            seataAutoProxyCreator.order = order - 1
        } else {
            LOGGER.info("kmodel-jdbc is used alone")
        }
    }

    override fun shouldSkip(
        beanClass: Class<*>,
        beanName: String
    ): Boolean =
        !DataSource::class.java.isAssignableFrom(beanClass) ||
                DataSourceProxy::class.java.isAssignableFrom(beanClass)

    override fun getAdvicesAndAdvisorsForBean(
        beanClass: Class<*>,
        beanName: String,
        customTargetSource: TargetSource?
    ): Array<Any> =
        arrayOf(advisor)
}

private val SETA_AUTO_PROXY_CREATOR_CLASS_NAME =
    "io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyCreator"

private val LOGGER =
    LoggerFactory.getLogger(DataSourceAutoProxyCreator::class.java)