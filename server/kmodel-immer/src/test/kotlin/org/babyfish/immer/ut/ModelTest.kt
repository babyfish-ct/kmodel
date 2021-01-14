package org.babyfish.immer.ut

import org.babyfish.immer.Department
import org.babyfish.kmodel.immer.runtime.ImmerRuntime
import java.lang.IllegalArgumentException
import kotlin.test.Test

class ModelTest {

    @Test
    fun printAsm() {
        ImmerRuntime.get(Department::class)
    }

    private fun map(key: String): String =
        when (key) {
            "I" -> "Alpha"
            "II" -> "Belta"
            "III" -> "Gamma"
            else -> throw IllegalArgumentException()
        }

    @Test
    fun test() {

//        val baseState = listOf(
//            newTask(
//                todo = "Learn kotlin",
//                done = true
//            ),
//            newTask(
//                todo = "Try immer",
//                done = false
//            )
//        )
//
//        val nextState = produce(baseState) {
//            draft.add(
//                newTask(
//                    todo = "Tweet about it",
//                    done = false
//                )
//            )
//            draft[1].done = true
//        }
    }


}