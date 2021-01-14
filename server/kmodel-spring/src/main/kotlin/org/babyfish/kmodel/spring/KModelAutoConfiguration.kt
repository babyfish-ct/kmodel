package org.babyfish.kmodel.spring

import org.babyfish.kmodel.spring.jdbc.DataSourceAutoProxyCreator
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class KModelAutoConfiguration(
    private val applicationContext: ApplicationContext
) {

    @Bean
    internal open fun dataSourceAutoProxyCreator(
    ): DataSourceAutoProxyCreator =
        DataSourceAutoProxyCreator(applicationContext)
}