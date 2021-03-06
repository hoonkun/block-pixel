package kiwi.hoonkun.plugins.pixel.nbt.tag

import kiwi.hoonkun.plugins.pixel.nbt.AnyTag
import kiwi.hoonkun.plugins.pixel.nbt.Tag
import kiwi.hoonkun.plugins.pixel.nbt.TagType.*
import kiwi.hoonkun.plugins.pixel.nbt.extensions.byte

import java.nio.ByteBuffer

class ByteTag private constructor(name: String? = null, parent: AnyTag?): Tag<Byte>(TAG_BYTE, name, parent) {

    override val sizeInBytes get() = Byte.SIZE_BYTES

    constructor(value: Byte, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.byte
    }

    override fun write(buffer: ByteBuffer) {
        buffer.put(value)
    }

    override fun clone(name: String?) = ByteTag(value, name, parent)

}