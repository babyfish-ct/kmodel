package org.babyfish.immer

import org.babyfish.kmodel.immer.Immer

@Immer
interface Task {
    val todo: String
    val done: Boolean
}

@Immer
interface TaskEx : Task {
    val link: String
}

@Immer
interface Department {
    val id: Long
    val name: String
    val employees: List<Employee>
}

@Immer
interface Employee {
    val id: Long
    val no: Int?
    val name: String
    val department: Department
    val supervisor: Employee?
}