package kiwi.hoonkun.plugins.pixel.nbt.tag

import kiwi.hoonkun.plugins.pixel.nbt.AnyTag
import kiwi.hoonkun.plugins.pixel.nbt.Tag
import kiwi.hoonkun.plugins.pixel.nbt.TagType.*
import java.nio.ByteBuffer

class IntArrayTag private constructor(name: String? = null, parent: AnyTag?): Tag<IntArray>(TAG_INT_ARRAY, name, parent) {

    override val sizeInBytes get() = Int.SIZE_BYTES + value.size * Int.SIZE_BYTES

    constructor(value: IntArray, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = IntArray(buffer.int) { buffer.int }
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        value.forEach { buffer.putInt(it) }
    }

    override fun clone(name: String?) = IntArrayTag(value, name, parent)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

}