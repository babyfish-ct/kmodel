package org.babyfish.immer.ut

import org.babyfish.immer.Department
import org.babyfish.immer.Employee

class DepartmentImpl(
    val idStateOrdinal: Int,
    val _id: Long?,
    val nameStateOrdinal: Int,
    val _name: String?,
    val employeesStateOrdinal: Int,
    val _employees: List<Employee>?
) : Department {

    override val id: Long
        get() =
            when (idStateOrdinal) {
                0 -> error("id未加载")
                1 -> error("id加载失败")
                else -> _id ?: error("内部BUG")
            }

    override val name: String
        get() =
            when (nameStateOrdinal) {
                0 -> error("name未加载")
                1 -> error("name加载失败")
                else -> _name ?: error("内部BUG")
            }

    override val employees: List<Employee>
        get() =
            when (employeesStateOrdinal) {
                0 -> error("employees未加载")
                1 -> error("employees加载失败")
                else -> _employees ?: error("内部BUG")
            }
}

class EmployeeImpl(
    val idStateOrdinal: Int,
    val _id: Long?,
    val noStateOrdinal: Int,
    val _no: Int?,
    val nameStateOrdinal: Int,
    val _name: String?,
    val departmentStateOrdinal: Int,
    val _department: Department?,
    val supervisorStateOrdinal: Int,
    val _supervisor: Employee?
) : Employee {

    override val id: Long
        get() =
            when (idStateOrdinal) {
                0 -> error("id未加载")
                1 -> error("id加载失败")
                else -> _id ?: error("内部BUG")
            }

    override val no: Int?
        get() =
            when (noStateOrdinal) {
                0 -> error("id未加载")
                1 -> error("id加载失败")
                else -> _no
            }

    override val name: String
        get() =
            when (nameStateOrdinal) {
                0 -> error("id未加载")
                1 -> error("id加载失败")
                else -> _name ?: error("内部BUG")
            }

    override val department: Department
        get() =
            when (departmentStateOrdinal) {
                0 -> error("id未加载")
                1 -> error("id加载失败")
                else -> _department ?: error("内部BUG")
            }

    override val supervisor: Employee?
        get() =
            when (supervisorStateOrdinal) {
                0 -> error("id未加载")
                1 -> error("id加载失败")
                else -> _supervisor
            }
}