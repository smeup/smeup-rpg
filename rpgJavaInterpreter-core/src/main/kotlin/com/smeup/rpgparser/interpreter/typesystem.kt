package com.smeup.rpgparser.interpreter

import com.smeup.rpgparser.parsing.ast.*
import com.smeup.rpgparser.parsing.parsetreetoast.RpgType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.max

// Supported data types:
// * Character Format
// * Numeric Data Type
// * UCS-2 Format
// * Date Data Type
// * Time Data Type
// * Timestamp Data Type
// * Object Data Type
// * Basing Pointer Data Type
// * Procedure Pointer Data Type

@Serializable
sealed class Type {
    open fun numberOfElements(): Int {
        return 1
    }
    open fun elementSize(): Int {
        return size
    }

    open fun canBeAssigned(value: Value): Boolean {
        return value.assignableTo(this)
    }

    open fun canBeAssigned(type: Type): Boolean {
        return this == type
    }

    abstract val size: Int

    fun toArray(nElements: Int) = ArrayType(this, nElements)
    fun isArray() = this is ArrayType
    open fun asArray(): ArrayType {
        throw IllegalStateException("Not an ArrayType")
    }
    open fun hasVariableSize() = false
}

@Serializable
object FigurativeType : Type() {
    override val size: Int
        get() = 0

    override fun canBeAssigned(value: Value): Boolean = true
}

@Serializable
object KListType : Type() {
    override val size: Int
        get() = 0

    override fun canBeAssigned(value: Value): Boolean = false
}

@Serializable
data class DataStructureType(val fields: List<FieldType>, val elementSize: Int) : Type() {
    override val size: Int
        get() = elementSize
}

@Serializable
data class StringType(val length: Int, val varying: Boolean = false) : Type() {
    override val size: Int
        get() = length
}
@Serializable
object BooleanType : Type() {
    override val size: Int
        get() = 1

    override fun toString() = this.javaClass.simpleName
}

@Serializable
object HiValType : Type() {
    override val size: Int
        get() = throw IllegalStateException("Has variable size")

    override fun hasVariableSize() = true
}

@Serializable
object LowValType : Type() {
    override val size: Int
        get() = throw IllegalStateException("Has variable size")

    override fun hasVariableSize() = true
}

@Serializable
object TimeStampType : Type() {
    override val size: Int
        get() = 26
}

/**
 * A CharacterType is basically very similar to an array of characters
 * and very similar to a string.
 */
@Serializable
data class CharacterType(val nChars: Int) : Type() {
    override val size: Int
        get() = nChars
}

infix fun Int.pow(exponent: Int): Long {
    require(exponent >= 0)
    return if (exponent == 0) {
        1
    } else {
        this * this.pow(exponent - 1)
    }
}

infix fun Long.log(base: Int): Double {
    return (Math.log(this.toDouble()) / Math.log(base.toDouble()))
}

@Serializable
data class NumberType(val entireDigits: Int, val decimalDigits: Int, val rpgType: String? = "") : Type() {

    constructor(entireDigits: Int, decimalDigits: Int, rpgType: RpgType) : this(entireDigits, decimalDigits, rpgType.rpgType)

    init {
        if (rpgType == RpgType.INTEGER.rpgType || rpgType == RpgType.UNSIGNED.rpgType) {
            require(entireDigits <= 20) { "Integer or Unsigned integer can have only length up to 20. Value specified: $this" }
            require(decimalDigits == 0)
        }
    }

    override val size: Int
        get() {
            return when (rpgType) {
                RpgType.PACKED.rpgType -> ceil((numberOfDigits + 1).toDouble() / 2.toFloat()).toInt()
                RpgType.INTEGER.rpgType, RpgType.UNSIGNED.rpgType -> {
                    when (entireDigits) {
                        in 1..3 -> 1
                        in 4..5 -> 2
                        in 6..10 -> 4
                        in 11..20 -> 8
                        else -> throw IllegalStateException("Only predefined length allowed for integer, signed or unsigned")
                    }
                }
                RpgType.BINARY.rpgType -> {
                    when (entireDigits) {
                        in 1..4 -> 2
                        in 5..9 -> 4
                        else -> throw IllegalStateException("Only predefined length allowed binary ")
                    }
                }
                else -> numberOfDigits
            }
        }

    val integer: Boolean
        get() = decimalDigits == 0
    val decimal: Boolean
        get() = !integer
    val numberOfDigits: Int
        get() = entireDigits + decimalDigits

    override fun canBeAssigned(type: Type): Boolean {
        if (type is NumberType) {
            return type.entireDigits <= this.entireDigits && type.decimalDigits <= this.decimalDigits
        } else {
            return false
        }
    }
}

@Serializable
data class ArrayType(val element: Type, val nElements: Int, val compileTimeRecordsPerLine: Int? = null) : Type() {
    var ascend: Boolean? = null

    override val size: Int
        get() = element.size * nElements

    override fun numberOfElements(): Int {
        return nElements
    }

    override fun elementSize(): Int {
        return element.size
    }

    override fun asArray(): ArrayType {
        return this
    }

    fun compileTimeArray(): Boolean = compileTimeRecordsPerLine != null
}

@Serializable
data class FieldType(val name: String, val type: Type)

fun Expression.type(): Type {
    return when (this) {
        is DataRefExpr -> {
            this.variable.referred!!.type
        }
        is StringLiteral -> {
            StringType(this.value.length, true) // TODO verify if varying has to be true or false here
        }
        is IntLiteral -> {
            NumberType(BigDecimal.valueOf(this.value).precision(), decimalDigits = 0)
        }
        is RealLiteral -> {
            NumberType(this.value.precision() - this.value.scale(), this.value.scale())
        }
        is ArrayAccessExpr -> {
            val type = this.array.type().asArray()
            return type.element
        }
        is IndicatorExpr -> {
            return BooleanType
        }
        is GlobalIndicatorExpr -> {
            return ArrayType(BooleanType, 99)
        }
        is HiValExpr -> {
            return HiValType
        }
        is LowValExpr -> {
            return LowValType
        }
        is SubstExpr -> {
            return this.string.type()
        }
        is QualifiedAccessExpr -> {
            return this.field.referred!!.type
        }
        is OnRefExpr, is OffRefExpr -> return BooleanType
        is FigurativeConstantRef -> {
            FigurativeType
        }
        is PlusExpr -> {
            val leftType = this.left.type()
            val rightType = this.right.type()
            if (leftType is NumberType && rightType is NumberType) {
                return NumberType(max(leftType.entireDigits, rightType.entireDigits), max(leftType.decimalDigits, rightType.decimalDigits))
            } else {
                TODO("We do not know the type of a sum of types $leftType and $rightType")
            }
        }
        else -> TODO("We do not know how to calculate the type of $this (${this.javaClass.canonicalName})")
    }
}

fun baseType(type: Type): Type = if (type is ArrayType) type.element else type
