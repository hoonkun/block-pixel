package kiwi.hoonkun.plugins.pixel.worker

import kiwi.hoonkun.plugins.pixel.Blocks
import kiwi.hoonkun.plugins.pixel.PackedBlocks
import kiwi.hoonkun.plugins.pixel.PackedSkyLight
import kiwi.hoonkun.plugins.pixel.SkyLight
import kotlin.experimental.and
import kotlin.math.ceil
import kotlin.math.pow

class ArrayPacker {

    companion object {

        private val FOUR_BIT_MASK = ((2.0).pow(4).toInt() - 1).toByte()

        fun PackedBlocks.unpack(paletteSize: Int): Blocks {
            val bitsPerBlock = size(paletteSize)
            val bitMask = (2.0).pow(bitsPerBlock).toLong() - 1L

            val result = mutableListOf<Int>()

            forEach { long ->
                var remaining = long
                for (block in 0 until(Long.SIZE_BITS / bitsPerBlock)) {
                    result.add((remaining and bitMask).toInt())
                    remaining = remaining shr bitsPerBlock

                    if (result.size == 4096) return@forEach
                }
            }

            return result
        }

        fun Blocks.pack(paletteSize: Int): PackedBlocks {
            val bitsPerBlock = size(paletteSize)
            val blocksPerLong = Long.SIZE_BITS / bitsPerBlock
            val totalLength = ceil(4096.0f / blocksPerLong).toInt()

            if (isEmpty()) return LongArray(0)

            return LongArray(totalLength) { index ->
                var long = 0L

                for (block in (index + 1) * blocksPerLong downToExclusive index * blocksPerLong) {
                    if (block >= 4096) continue
                    if (block != (index + 1) * blocksPerLong) long = long shl bitsPerBlock
                    long = long or get(block).toLong()
                }

                long
            }
        }

        fun PackedSkyLight.unpack(): SkyLight {
            val result = mutableListOf<Byte>()

            forEach { byte ->
                var remaining = byte
                for (block in 0 until 2) {
                    result.add(remaining and FOUR_BIT_MASK)
                    remaining = (remaining.toInt() shr 4).toByte()
                }
            }

            return result
        }

        private fun size(size: Int): Int {
            var result = 4
            var value = 2 * 2 * 2 * 2
            while (size > value) {
                value *= 2
                result++
            }
            return result
        }

        private infix fun Int.downToExclusive(other: Int): IntProgression {
            return (this - 1).downTo(other)
        }

    }

}