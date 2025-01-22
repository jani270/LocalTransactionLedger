package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column
import moe.nea.ledger.database.DBType

class ClauseBuilder {
	fun <T, R> column(column: Column<T, R>): ColumnOperand<T, R> = ColumnOperand(column)
	fun string(string: String): StringOperand = StringOperand(string)
	fun <T, R> value(dbType: DBType<T, R>, value: T): Operand<T, R> = ValuedOperand(dbType, value)
	infix fun <T> Operand<*, T>.eq(operand: Operand<*, T>): Clause = EqualsClause(this, operand)
	infix fun <T, R> TypedOperand<T, R>.eq(value: T): Clause = EqualsClause(this, value(dbType, value))
	infix fun Operand<*, String>.like(op: StringOperand): Clause = LikeClause(this, op)
	infix fun Operand<*, String>.like(op: String): Clause = LikeClause(this, string(op))
	infix fun <T> Operand<*, T>.lt(op: Operand<*, T>): BooleanExpression = LessThanExpression(this, op)
	infix fun <T> Operand<*, T>.le(op: Operand<*, T>): BooleanExpression = LessThanEqualsExpression(this, op)
	infix fun <T> Operand<*, T>.gt(op: Operand<*, T>): BooleanExpression = op lt this
	infix fun <T> Operand<*, T>.ge(op: Operand<*, T>): BooleanExpression = op le this
	infix fun BooleanExpression.and(clause: BooleanExpression): BooleanExpression = ANDExpression(listOf(this, clause))
	infix fun BooleanExpression.or(clause: BooleanExpression): BooleanExpression = ORExpression(listOf(this, clause))
}