package kiwi.hoonkun.plugins.pixel.nbt.tag

import kiwi.hoonkun.plugins.pixel.nbt.AnyTag
import kiwi.hoonkun.plugins.pixel.nbt.Tag
import kiwi.hoonkun.plugins.pixel.nbt.TagType
import kiwi.hoonkun.plugins.pixel.nbt.TagType.*
import kiwi.hoonkun.plugins.pixel.nbt.extensions.byte
import kiwi.hoonkun.plugins.pixel.nbt.extensions.indent
import java.nio.ByteBuffer

class ListTag private constructor(name: String? = null, parent: AnyTag?): Tag<List<AnyTag>>(TAG_LIST, name, parent) {

    override val sizeInBytes: Int
        get() = Byte.SIZE_BYTES + Int.SIZE_BYTES + value.sumOf { it.sizeInBytes }

    lateinit var elementsType: TagType private set

    operator fun get(index: Int) = value[index]

    constructor(elementsType: TagType, value: List<AnyTag>, typeCheck: Boolean = true, name: String? = null, parent: AnyTag?): this(name, parent) {
        require(!typeCheck || typeCheck(elementsType, value)) { "ListTag's elements must be of a single type" }

        this.elementsType = elementsType
        this.value = value.map { tag -> tag.ensureName(null) }.toList()
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    private fun typeCheck(elementsType: TagType, list: List<AnyTag>) = list.all { it.type == elementsType }

    override fun read(buffer: ByteBuffer) {
        elementsType = TagType[buffer.byte]
        value = List(buffer.int) { read(elementsType, buffer, null, this).apply { indexInList = it } }
    }

    override fun write(buffer: ByteBuffer) {
        buffer.put(elementsType.id)
        buffer.putInt(value.size)
        value.forEach { it.write(buffer) }
    }

    override fun clone(name: String?) = ListTag(elementsType, value.map { it.clone(null) }, false, name, parent)

    override fun valueToString(): String = if (value.isEmpty()) "[]" else "[\n${value.joinToString(",\n") { it.toString() }.indent()}\n]"

    fun generateTypes(parentPath: String = ""): Map<String, Byte> {
        if (elementsType != TAG_COMPOUND) return mapOf()

        val result = mutableMapOf<String, Byte>()
        value.mapIndexed { index, tag ->
            result.putAll(tag.getAs<CompoundTag>().generateTypes("$parentPath[$index]"))
        }
        return result
    }

}