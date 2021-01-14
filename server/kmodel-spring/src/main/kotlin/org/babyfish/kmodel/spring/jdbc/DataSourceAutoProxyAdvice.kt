package org.babyfish.kmodel.spring.jdbc

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.IntroductionInfo

internal class DataSourceAutoProxyAdvice : MethodInterceptor, IntroductionInfo {

    override fun invoke(invocation: MethodInvocation): Any? {
        if (invocation.method.name != "getConnection") {
            return invocation.proceed()
        }
        println("Before ${invocation.method}")
        return try {
            invocation.proceed()
        } finally {
            println("After ${invocation.method}")
        }.also {
            println("Intercepted type: ${it::class.qualifiedName}")
        }
    }

    override fun getInterfaces(): Array<Class<*>> =
        arrayOf(DataSourceProxy::class.java)
}