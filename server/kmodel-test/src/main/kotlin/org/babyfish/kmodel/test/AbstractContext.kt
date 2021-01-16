package org.babyfish.kmodel.test

import org.junit.ComparisonFailure
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.reflect.KProperty1
import kotlin.test.DefaultAsserter
import kotlin.test.fail

abstract class AbstractContext<T> internal constructor(
    private val parent: AbstractContext<*>?,
    private val parentProp: Any?,
    val value: T
) {
    private val toStringResult: String by lazy {
        parent
            ?.let {
                "$it${memberSuffix(parentProp!!)}"
            }
            ?: this::class.simpleName!!
    }

    override fun toString(): String = toStringResult

    infix fun eq(value: T?) {
        if (this.value != value) {
            if (this.value is BigDecimal &&
                value is BigDecimal &&
                this.value.compareTo(value) == 0) {
                return
            }
            throw ComparisonFailure(
                "Illegal value of $this",
                value.toString(),
                this.value.toString()
            )
        }
    }

    fun eq(valueSupplier: () -> T?) {
        eq(valueSupplier())
    }

    infix fun ne(value: T?) {
        DefaultAsserter.assertTrue(
            lazyMessage = {
                "Illegal value of $this, expected value should not be <$value>"
            },
            actual = this.value != value
        )
    }

    fun ne(valueSupplier: () -> T?) {
        ne(valueSupplier())
    }

    infix fun same(value: T?) {
        DefaultAsserter.assertTrue(
            lazyMessage = {
                "Illegal reference of $this, expect <$value>, actual <${this.value}>."
            },
            actual = this.value === value
        )
    }

    fun same(valueSupplier: () -> T?) {
        same(valueSupplier())
    }

    fun <X> value(
        calculationRuleName: String,
        calculator: T.() -> X
    ): ValueContext<X> =
        CalculationRule(calculationRuleName, calculator).let {
            ValueContext(
                this,
                it,
                it.calculate(value)
            )
        }

    fun <X> obj(
        calculationRuleName: String,
        calculator: T.() -> X,
        contextAction: ObjectContext<X>.() -> Unit
    ) {
        CalculationRule(calculationRuleName, calculator).let {
            ObjectContext(
                this,
                it,
                it.calculate(value)
            ).contextAction()
        }
    }

    fun <E> list(
        calculationRuleName: String,
        calculator: T.() -> Iterable<E>?,
        contextAction: ListContext<E>.() -> Unit
    ) {
        CalculationRule(calculationRuleName, calculator).let {
            ListContext(
                this,
                it,
                it.calculate(value)?.asList()
            ).contextAction()
        }
    }

    fun <K, V> map(
        calculationRuleName: String,
        calculator: T.() -> Map<K, V>?,
        contextAction: MapContext<K, V>.() -> Unit
    ) {
        CalculationRule(calculationRuleName, calculator).let {
            MapContext(
                this,
                it,
                it.calculate(value)
            ).contextAction()
        }
    }

    private fun memberSuffix(prop: Any?): String =
        when (prop) {
            is KProperty1<*, *> -> ".${prop.name}"
            is Number -> "[$prop]"
            is Boolean -> "[$prop]"
            is Char -> "[$'prop']"
            is CalculationRule<*, *> -> ".${prop.name}(calculator = ${prop.calculator::class})"
            null -> "[null]"
            else -> "[\"$prop\"]"
        }

    internal fun member(prop: Any?): String =
        "$this${memberSuffix(prop)}"

    private class CalculationRule<T, X>(
        val name: String,
        val calculator: T.() ->X
    ) {
        init {
            if (!CALCULATION_RULE_PATTERN.matcher(name).matches()) {
                throw IllegalArgumentException(
                    "The argument name is \"$name\", " +
                            "it does not match the regular expression " +
                            "\"${CALCULATION_RULE_PATTERN.pattern()}\""
                )
            }
        }

        fun calculate(value: T?): X {
            if (value === null) {
                fail("Cannot execute the calculation on $this because it's null")
            }
            return value.calculator()
        }
    }
}

private val CALCULATION_RULE_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*")
