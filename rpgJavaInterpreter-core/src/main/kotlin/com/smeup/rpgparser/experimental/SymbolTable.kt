package com.smeup.rpgparser.experimental

import com.smeup.rpgparser.fastmaps.IntArrayMap
import com.smeup.rpgparser.interpreter.*

/**
 * Experimental SymbolTable.
 * Data are stored IntArrayMap, which is a more efficient map than standard ones
 * @see IntArrayMap
 * */
class SymbolTable : ISymbolTable {
    private val values = IntArrayMap<Pair<AbstractDataDefinition, Value>>(0, 32000)
    private val names = mutableMapOf<String, AbstractDataDefinition>()

    override operator fun contains(dataName: String): Boolean = dataDefinitionByName(dataName) != null
    override operator fun contains(data: AbstractDataDefinition): Boolean = data.key in values.keys

    override operator fun get(data: AbstractDataDefinition): Value {
        if (data is FieldDefinition) {
            val containerValue = get(data.container)
            return if (data.container.isArray()) {
                TODO("We do not yet handle an array container")
            } else if (data.declaredArrayInLine != null) {
                ProjectedArrayValue.forData(containerValue as DataStructValue, data)
            } else {
                // Should be always a DataStructValue
                if (containerValue is DataStructValue) {
                    return coerce(containerValue[data], data.type)
                } else {
                    throw IllegalStateException("The container value is expected to be a DataStructValue, instead it is $containerValue")
                }
            }
        }
        val pair = values[data.key]
        require(pair != null) { "Cannot find searched value for $data" }
        require(pair.first.name == data.name) { "I have retrieved ${pair.first} instead of ${data.name}" }
        return pair.second
    }

    override operator fun get(dataName: String): Value {
        val data = dataDefinitionByName(dataName)
        if (data != null) {
            val variable = values[data.key]
            return variable?.second ?: throw IllegalArgumentException("Cannot find searched value for $data")
        }

        // We did not find a top-level declaration with that name,
        // looking among fields
        for (topLevelValue in values.values) {
            if (topLevelValue.first is DataDefinition) {
                val field = (topLevelValue.first as DataDefinition).fields.firstOrNull {
                    it.name.equals(dataName, ignoreCase = true) && it.canBeUsedUnqualified()
                }
                if (field != null) {
                    return if (topLevelValue.first.type is ArrayValue) {
                        TODO("We do not yet handle top level values of array type")
                    } else {
                        (topLevelValue.second as DataStructValue)[field]
                    }
                }
            }
        }
        throw IllegalArgumentException("Cannot find searchedValued for $dataName")
    }

    override fun dataDefinitionByName(dataName: String) = names[dataName.toUpperCase()]

    override operator fun set(data: AbstractDataDefinition, value: Value): Value? {
        names[data.name.toUpperCase()] = data
        return values.put(data.key, Pair(data, value.forType(data.type)))?.second
    }

    override fun getValues(): Map<AbstractDataDefinition, Value> = values.values.toMap()

    /**
     * Clear symbol table
     * */
    override fun clear() {
        values.clear()
        names.clear()
    }

    /**
     * @return if is empty
     * */
    override fun isEmpty() = values.isEmpty()
}