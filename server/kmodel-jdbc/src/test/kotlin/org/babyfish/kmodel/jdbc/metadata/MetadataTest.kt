package org.babyfish.kmodel.jdbc.metadata

import org.babyfish.kmodel.jdbc.AbstractJdbcTest
import org.babyfish.kmodel.test.*
import org.junit.Test
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
import kotlin.test.assertFailsWith
import kotlin.test.expect

class MetadataTest : AbstractJdbcTest() {

    override fun setupDatabase() {
        connection.createStatement().apply {
            addBatch(
                """
                create table account(
                    bank_code varchar(5) not null,
                    user_code varchar(16) not null,
                    residual_amount int not null,
                    constraint pk_account 
                        primary key(bank_code, user_code) 
                )
                """
            )
            addBatch(
                """create table transaction(
                    id bigint not null,
                    source_bank_code varchar(5) not null,
                    source_user_code varchar(16) not null,
                    target_bank_code varchar(5) not null,
                    target_user_code varchar(16) not null,
                    amount int not null,
                    constraint pk_transaction
                        primary key(id),
                    constraint fk_transaction_source
                        foreign key(source_bank_code, source_user_code)
                            references account(bank_code, user_code),
                    constraint fk_transaction_target
                        foreign key(target_bank_code, target_user_code)
                            references account(bank_code, user_code)
                )"""
            )
            addBatch(
                """create table tree(
                    id bigint not null,
                    name varchar(20) not null,
                    parent_id bigint,
                    constraint pk_tree
                        primary key(id),
                    constraint fk_tree_parent
                        foreign key(parent_id)
                            references tree(id)
                )"""
            )
            addBatch(
                """create table illegal_tree(
                    id bigint not null,
                    parent_id bigint,
                    constraint pk_illegal_tree
                        primary key(id),
                    constraint fk_illegal_tree_parent
                        foreign key(parent_id)
                            references illegal_tree(id)
                                on delete cascade
                ) """
            )
            executeBatch()
        }
    }

    @Test
    fun testAccountMetadata() {

        evictTableManagers()

        // Access parent table earlier than accessing child table
        val table = tableManager(connection)[connection, "account"]
        val childTable = tableManager(connection)[connection, "transaction"]

        expectObj(table) {
            obj(Table::qualifiedName) {
                value(QualifiedName::catalog) eq "METADATA-TEST"
                value(QualifiedName::schema) eq "PUBLIC"
                value(QualifiedName::name) eq "ACCOUNT"
            }
            map(Table::columnMap) {
                assertAccountColumnMap()
            }
            map(Table::insensitiveColumnMap) {
                assertAccountColumnMap()
            }
            map(Table::primaryKeyColumnMap) {
                assertAccountPkColumnMap(table)
            }
            map(Table::insensitivePrimaryKeyColumnMap) {
                assertAccountPkColumnMap(table)
            }
            list(Table::importedForeignKeys) {
                size(0)
            }
            list(Table::exportedForeignKeys) {
                assertForeignKeys(
                    transactionTable = childTable,
                    accountTable = table
                )
            }
        }
    }

    @Test
    fun testTransactionMetadata() {

        evictTableManagers()

        // Access child table earlier than accessing parent table
        val table = tableManager(connection)[connection, "transaction"]
        val parentTable = tableManager(connection)[connection, "account"]

        expectObj(table) {
            obj(Table::qualifiedName) {
                value(QualifiedName::catalog) eq "METADATA-TEST"
                value(QualifiedName::schema) eq "PUBLIC"
                value(QualifiedName::name) eq "TRANSACTION"
            }
            map(Table::columnMap) {
                assertTransactionColumnMap()
            }
            map(Table::insensitiveColumnMap) {
                assertTransactionColumnMap()
            }
            map(Table::primaryKeyColumnMap) {
                assertTransactionPkColumnMap(table)
            }
            map(Table::insensitivePrimaryKeyColumnMap) {
                assertTransactionPkColumnMap(table)
            }
            list(Table::importedForeignKeys) {
                assertForeignKeys(
                    transactionTable = table,
                    accountTable = parentTable
                )
            }
            list(Table::exportedForeignKeys) {
                size(0)
            }
        }
    }

    private fun MapContext<String, Column>.assertAccountColumnMap() {
        size(3)
        obj("BANK_CODE") {
            value(Column::name) eq "BANK_CODE"
            value(Column::type) eq Types.VARCHAR
        }
        obj("USER_CODE") {
            value(Column::name) eq "USER_CODE"
            value(Column::type) eq Types.VARCHAR
        }
        obj("RESIDUAL_AMOUNT") {
            value(Column::name) eq "RESIDUAL_AMOUNT"
            value(Column::type) eq Types.INTEGER
        }
    }

    private fun MapContext<String, Column>.assertAccountPkColumnMap(table: Table) {
        size(2)
        obj("BANK_CODE") {
            same(table.columnMap["BANK_CODE"])
        }
        obj("USER_CODE") {
            same(table.columnMap["USER_CODE"])
        }
    }

    private fun MapContext<String, Column>.assertTransactionColumnMap() {
        size(6)
        obj("ID") {
            value(Column::name) eq "ID"
            value(Column::type) eq Types.BIGINT
        }
        obj("SOURCE_BANK_CODE") {
            value(Column::name) eq "SOURCE_BANK_CODE"
            value(Column::type) eq Types.VARCHAR
        }
        obj("SOURCE_USER_CODE") {
            value(Column::name) eq "SOURCE_USER_CODE"
            value(Column::type) eq Types.VARCHAR
        }
        obj("TARGET_BANK_CODE") {
            value(Column::name) eq "TARGET_BANK_CODE"
            value(Column::type) eq Types.VARCHAR
        }
        obj("TARGET_USER_CODE") {
            value(Column::name) eq "TARGET_USER_CODE"
            value(Column::type) eq Types.VARCHAR
        }
        obj("AMOUNT") {
            value(Column::name) eq "AMOUNT"
            value(Column::type) eq Types.INTEGER
        }
    }

    private fun MapContext<String, Column>.assertTransactionPkColumnMap(transactionTable: Table) {
        size(1)
        obj("ID") {
            same(transactionTable.columnMap["ID"])
        }
    }

    private fun ListContext<ForeignKey>.assertForeignKeys(
        transactionTable: Table,
        accountTable: Table
    ) {
        size(2)
        obj(0) {
            value(ForeignKey::name) eq "FK_TRANSACTION_SOURCE"
            obj(ForeignKey::childTable) {
                same(transactionTable)
            }
            obj(ForeignKey::parentTable) {
                same(accountTable)
            }
            list(ForeignKey::childTableColumns) {
                size(2)
                obj(0) {
                    same(transactionTable.columnMap["SOURCE_BANK_CODE"])
                }
                obj(1) {
                    same(transactionTable.columnMap["SOURCE_USER_CODE"])
                }
            }
            list(ForeignKey::parentTableColumns) {
                size(2)
                obj(0) {
                    same(accountTable.columnMap["BANK_CODE"])
                }
                obj(1) {
                    same(accountTable.columnMap["USER_CODE"])
                }
            }
        }
    }

    @Test
    fun testTreeMetadata() {

        evictTableManagers()

        val table = tableManager(connection)[connection, "tree"]

        expectObj(table) {
            obj(Table::qualifiedName) {
                value(QualifiedName::catalog) eq "METADATA-TEST"
                value(QualifiedName::schema) eq "PUBLIC"
                value(QualifiedName::name) eq "TREE"
            }
            map(Table::columnMap) {
                size(3)
                obj("ID") {
                    value(Column::name) eq "ID"
                    value(Column::type) eq Types.BIGINT
                }
                obj("NAME") {
                    value(Column::name) eq "NAME"
                    value(Column::type) eq Types.VARCHAR
                }
                obj("PARENT_ID") {
                    value(Column::name) eq "PARENT_ID"
                    value(Column::type) eq Types.BIGINT
                }
            }
            list(Table::primaryKeyColumns) {
                size(1)
                value(0) same table.columnMap["ID"]
            }
            list(Table::importedForeignKeys) {
                size(1)
                obj(0) {
                    value(ForeignKey::name) eq "FK_TREE_PARENT"
                    value(ForeignKey::childTable) same table
                    value(ForeignKey::parentTable) same table
                    list(ForeignKey::childTableColumns) {
                        size(1)
                        value(0) same table.columnMap["PARENT_ID"]
                    }
                    list(ForeignKey::parentTableColumns) {
                        size(1)
                        value(0) same table.columnMap["ID"]
                    }
                }
            }
            list(Table::exportedForeignKeys) {
                size(1)
                obj(0) {
                    value(ForeignKey::name) eq "FK_TREE_PARENT"
                    value(ForeignKey::childTable) same table
                    value(ForeignKey::parentTable) same table
                    list(ForeignKey::childTableColumns) {
                        size(1)
                        value(0) same table.columnMap["PARENT_ID"]
                    }
                    list(ForeignKey::parentTableColumns) {
                        size(1)
                        value(0) same table.columnMap["ID"]
                    }
                }
            }
        }
    }

    @Test
    fun testIllegalTree() {

        evictTableManagers()

        expect(
            "Illegal foreign key constraints [" +
                    "METADATA-TEST.PUBLIC.FK_ILLEGAL_TREE_PARENT" +
                    "], 'on delete set null' or 'on delete cascade' " +
                    "are not supported by kmodel-jdbc"
        ) {
            assertFailsWith(SQLException::class) {
                tableManager(connection)[connection, "illegal_tree"]
            }.message
        }
    }
}