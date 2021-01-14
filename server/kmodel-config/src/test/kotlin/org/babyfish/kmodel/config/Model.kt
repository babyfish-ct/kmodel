package org.babyfish.kmodel.config

import java.math.BigDecimal

interface Department {
    val id: Long
    val name: String
    val employees: List<Employee> //直属员工集合
    val pluralisticEmployees: List<Employee> //兼职员工集合
    val avgSalary: BigDecimal //计算字段：直属员工平均薪资
    val pluralisticAvgSalary: BigDecimal //计算字段：兼职员工平均薪资
    val mixedAvgSalary: BigDecimal //计算字段：所有员工平均薪资
    val deleted: Boolean
}

interface Employee {
    val id: Long
    val name: String
    val salary: BigDecimal
    val department: Department //直属部门
    val pluralisticDepartments: List<Department> //兼职部门集合
    val deleted: Boolean
}

val departmentModel = Model.create(Department::class) {

    filter {
        match(Department::deleted, false)
    }

    sql {
        tableName("DEPARTMENT")
    }

    graphql {
        batchSize(100)
    }

    redis {
        keyPrefix("department-")
    }

    id(Department::id) {
        sql {
            column("DEPARTMENT_ID")
        }
    }

    value(Department::name)

    oneToMany(Department::employees) {
        mappedBy(Employee::department)
        graphql {
            batchSize(16)
        }
        redis {
            keyPrefix("employee-ids-of-department-")
        }
    }

    manyToMany(Department::pluralisticEmployees) {
        mappedBy(Employee::pluralisticDepartments)
        graphql {
            batchSize(16)
        }
        redis {
            keyPrefix("pluralistic-employee-ids-of-department-")
        }
    }

    computed(Department::avgSalary) {
        avg(Department::employees, Employee::salary)
    }

    computed(Department::pluralisticAvgSalary) {
        avg(Department::pluralisticEmployees, Employee::salary)
    }

    computed(Department::mixedAvgSalary) {
        aggregation(
            initializedAccumulator = object: Accumulator<Long, BigDecimal> {
                private val employeeSalaryMap = mutableMapOf<Long, BigDecimal>()

                override fun accumulate(key: Long, value: BigDecimal) {
                    employeeSalaryMap[key] = value
                }

                override fun calculate(): BigDecimal = (
                        employeeSalaryMap.values.sumByDouble { it.toDouble() } /
                                employeeSalaryMap.size
                        ).toBigDecimal()
            },
            elementKeyProperty = Employee::id,
            elementValueProperty = Employee::salary
        ) {
            listOf(
                Department::employees,
                Department::pluralisticEmployees
            )
        }
    }
}

val employeeModel = Model.create(Employee::class) {

    filter {
        match(Employee::deleted, false)
    }

    id (Employee::id) {
        sql {
            column("DEPARTMENT_ID")
        }
    }

    value(Employee::name)

    value(Employee::salary) {
        validation {
            minValue(1_000)
            maxValue(100_000)
        }
    }

    toOne(Employee::department) {
        sql {
            column("DEPARTMENT_ID")
            onDeleteCascade()
        }
    }
}
