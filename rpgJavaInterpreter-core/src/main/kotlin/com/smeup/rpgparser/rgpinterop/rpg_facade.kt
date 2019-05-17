package com.smeup.rpgparser.rgpinterop

import com.smeup.rpgparser.interpreter.*
import com.smeup.rpgparser.jvminterop.JavaSystemInterface
import com.smeup.rpgparser.jvminterop.Size
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

annotation class Param(val name: String)

private val <R> KProperty<R>.rpgName: String
    get() {
        val param = this.findAnnotation<Param>()
        return param?.name ?: this.name
    }

interface ProgramNameSource<P> {
    fun nameFor(rpgFacade: RpgFacade<P>) : String
}

class ClassProgramName<P> : ProgramNameSource<P> {
    override fun nameFor(rpgFacade: RpgFacade<P>) : String = rpgFacade.javaClass.simpleName
}

abstract class RpgFacade<P> (val programNameSource: ProgramNameSource<P> = ClassProgramName<P>()) {

    var traceMode = false

    protected val programInterpreter = ProgramInterpreter(JavaSystemInterface)
    protected val rpgProgram by lazy { RpgSystem.getProgram(programNameSource.nameFor(this)) }

    fun singleCall(params: P) : P? {
        programInterpreter.execute(rpgProgram, toInitialValues(params), traceMode = traceMode)
        // TODO populate P correctly
        return null
    }

    open protected fun toInitialValues(params: P) : Map<String, Value> {
        val any : Any = params!!
        val kclass = any::class
        val initialValues = HashMap<String, Value>()
        kclass.memberProperties.forEach {
            initialValues[it.rpgName] = toRpgValue(it, it.call(params))
        }
        return initialValues
    }

    private fun propertyStringValue(property: KProperty1<Any, *>, container: Any) : String {
        val value = property.get(container)
        if (value is String) {
            return value
        } else {
            TODO()
        }
    }

    private fun toRpgValue(property: KType, jvmValue: Any?): Value {
        return when {
            jvmValue is String -> StringValue(jvmValue)
            else -> {
                val rpgType = property.toRpgType()
                if (jvmValue is String) {
                    StringValue(jvmValue)
                } else {
                    val parts = LinkedList<String>()
                    jvmValue!!.javaClass.kotlin.memberProperties.forEach {
                        val stringLength = (it.rpgType() as StringType).length.toInt()
                        parts.add(propertyStringValue(it, jvmValue).padEnd(stringLength, '\u0000'))
                    }
                    return StringValue(parts.joinToString(separator = ""))
                }
            }
        }
    }

    private fun toRpgValue(property: KProperty<*>, jvmValue: Any?): Value {
        return when {
            property.returnType == String::class.createType() -> {
                val s = jvmValue as String
                StringValue(s)
            }
            property.returnType.classifier is KClass<*> && (property.returnType.classifier as KClass<*>).qualifiedName == "kotlin.Array" -> {
                val jvmArray = jvmValue as Array<*>
                val elementType = property.returnType.arguments[0].type!!
                val nElements = property.findAnnotation<Size>()?.size ?: throw RuntimeException("Size expected for property ${property.name}")
                val rpgArray = createArrayValue(elementType.toRpgType(), nElements) {
                    if (it < jvmArray.size) {
                        toRpgValue(elementType, jvmArray[it])
                    } else {
                        blankValue(elementType.toRpgType())
                    }
                }
                return rpgArray
            }
            else ->{
                val classifier = property.returnType.classifier
                println((classifier as KClass<*>).qualifiedName == "kotlin.Array")
                val array = Array<Any>::class
                TODO("Property ${property}")
            }
        }
    }

}

private fun KType.toRpgType(size: Size? = null): Type {
    return when {
        this.classifier == String::class -> {
            StringType(size!!.size.toLong())
        }
        this.classifier is KClass<*> -> {
            val length = (this.classifier as KClass<*>).memberProperties.map { it.rpgLength() }.foldRight(0L) { it, acc -> it + acc }
            StringType(length)
        }
        else -> TODO("$this")
    }
}

private fun <R> KProperty<R>.rpgLength(): Long {
    val size = this.findAnnotation<Size>()
    if (size != null) {
        return size.size.toLong()
    }
    return this.returnType.toRpgType().size
}

private fun <R> KProperty<R>.rpgType(): Type {
    val size = this.findAnnotation<Size>()
    return this.returnType.toRpgType(size)
}