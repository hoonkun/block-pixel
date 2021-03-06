package kiwi.hoonkun.plugins.pixel

import kiwi.hoonkun.plugins.pixel.nbt.TagType
import kiwi.hoonkun.plugins.pixel.nbt.tag.*
import kiwi.hoonkun.plugins.pixel.worker.MergeWorker
import java.io.File

/** 월드 하나의 모든 ByteArray 타입의 MinecraftAnvil 파일 데이터를 포함하는 데이터 구조 */
typealias WorldAnvilFormat = Map<AnvilType, AnvilFormat>
/** AnvilType 하나의 모든 ByteArray MinecraftAnvil 파일 데이터를 포함하는 데이터 구조 */
typealias AnvilFormat = Map<AnvilLocation, ByteArray>

/** 변경 불가능한, AnvilType(T) 하나의 MinecraftAnvil NBT 데이터를 포함하는 데이터 구조 */
typealias Anvil<T/* :ChunkData */> = Map<AnvilLocation, List<T>>
/** 변경 가능한, AnvilType(T) 하나의 MinecraftAnvil NBT 데이터를 포함하는 데이터 구조 */
typealias MutableAnvil<T/* :ChunkData */> = MutableMap<AnvilLocation, MutableList<T>>

/** Packed 된 블럭 데이터 */
typealias PackedBlocks = LongArray
/** unpacked 된 블럭 데이터. 일반적으로 그 길이는 4096이다. */
typealias Blocks = List<Int>

/** Packed 된 SkyLight 데이터 */
typealias PackedSkyLight = ByteArray
typealias SkyLight = List<Byte>

/** Anvil 의 위치. 파일이름에 포함되는 그것이다. 예를 들어 r.-1.0.mca 일 경우 x는 -1, z는 0이 된다. */
data class AnvilLocation(val x: Int, val z: Int)
/** Chunk 의 위치. Anvil 에 상대적이지 않은 청크 좌표계의 절대 위치이다. */
data class ChunkLocation(val x: Int, val z: Int) {
    fun toRelative(base: AnvilLocation): RelativeChunkLocation
        = RelativeChunkLocation(x - base.x * 32, z - base.z * 32)
}
/** Anvil 에 상대적인 Chunk 의 위치. */
data class RelativeChunkLocation(val x: Int, val z: Int)

/** 버전관리가 진행되는 Anvil 의 타입들 */
enum class AnvilType(private val path: String) {
    TERRAIN("region"), POI("poi"), ENTITY("entities");

    fun getClient(worldName: String): String {
        val worldDir = "${Entry.clientFolder.absolutePath}/$worldName"
        val dimDir = File(worldDir).listFiles()?.find { it.name.contains("DIM") }

        return if (dimDir == null) "$worldDir/$path"
        else "$worldDir/${dimDir.name}/$path"
    }

    fun getRepository(worldName: String): String =
        "${Entry.repositoryFolder.absolutePath}/$worldName/$path"

    fun getMergeSpace(targetType: MergeWorker.TargetType): String =
        "${Entry.mergeFolder.absolutePath}/${targetType.path}/$path"
}

/** 월드 하나의 모든 MinecraftAnvil NBT 데이터를 포함하는 데이터 구조 */
data class WorldAnvil(
    val entity: Anvil<Entity>,
    val poi: Anvil<Poi>
)

/** Anvil 파일 하나를 구성하는 여러 종류의 Chunk 데이터를 아우르는 추상 클래스.
 *  이 클래스 인스턴스 하나는 Anvil 파일 하나를 구성하는 단 한 종류의 데이터들 중 하나를 지칭한다. */
abstract class ChunkData(val timestamp: Int, val nbt: CompoundTag) {
    abstract val location: ChunkLocation
}

/** Terrain 타입의 Anvil 파일을 구성하는 Chunk 데이터를 갖는 데이터 구조 */
class Terrain(timestamp: Int, nbt: CompoundTag): ChunkData(timestamp, nbt) {
    override val location = ChunkLocation(xPos, zPos)

    private val xPos get() = nbt["xPos"]!!.getAs<IntTag>().value
    private val zPos get() = nbt["zPos"]!!.getAs<IntTag>().value

    val sections = nbt["sections"]!!.getAs<ListTag>().value.map { Section(it.getAs()) }
    var blockEntities
        get() = nbt["block_entities"]!!.getAs<ListTag>().value.map { BlockEntity(it.getAs()) }
        set(value) {
            nbt["block_entities"] = ListTag(TagType.TAG_COMPOUND, value.map { it.nbt }, true, "block_entities", nbt)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Terrain

        if (location != other.location) return false
        if (xPos != other.xPos) return false
        if (zPos != other.zPos) return false
        if (sections != other.sections) return false
        if (blockEntities != other.blockEntities) return false

        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + xPos
        result = 31 * result + zPos
        result = 31 * result + sections.hashCode()
        result = 31 * result + blockEntities.hashCode()
        return result
    }
}

data class Section(private val nbt: CompoundTag) {
    val y: Byte get() {
        val yTag = nbt["Y"]
        if (yTag is IntTag) {
            nbt["Y"] = ByteTag(yTag.value.toByte(), "Y", nbt)
            return yTag.value.toByte()
        }
        return yTag!!.getAs<ByteTag>().value
    }
    val blockStates = if (nbt["block_states"] != null) BlockStates(nbt["block_states"]!!.getAs()) else null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Section

        if (y != other.y) return false
        if (blockStates != other.blockStates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = y.toInt()
        result = 31 * result + blockStates.hashCode()
        return result
    }
}

data class BlockStates(private val nbt: CompoundTag) {
    var palette
        get() = nbt["palette"]!!.getAs<ListTag>().value.map {
            Palette(it.getAs())
        }
        set(value) {
            nbt["palette"] = ListTag(TagType.TAG_COMPOUND, value.map { it.nbt }, true, "palette", nbt)
        }
    var data: PackedBlocks
        get() = nbt["data"]?.getAs<LongArrayTag>()?.value ?: LongArray(0)
        set(value) {
            nbt["data"] = LongArrayTag(value, "data", nbt)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockStates

        if (palette != other.palette) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = palette.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class Palette(val nbt: CompoundTag) {
    val name = nbt["Name"]!!.getAs<StringTag>().value
    val properties = nbt["Properties"]?.getAs<CompoundTag>()?.value?.map { it.key to it.value.getAs<StringTag>().value }?.toMap()

    override fun equals(other: Any?): Boolean {
        return name == (other as? Palette)?.name && properties == (other as? Palette)?.properties
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }
}

data class BlockEntity(val nbt: CompoundTag) {
    val x = nbt["x"]!!.getAs<IntTag>().value
    val y = nbt["y"]!!.getAs<IntTag>().value
    val z = nbt["z"]!!.getAs<IntTag>().value
}

/** Poi 타입의 Anvil 파일을 구성하는 Chunk 데이터를 갖는 데이터 구조 */
class Poi(override val location: ChunkLocation, timestamp: Int, nbt: CompoundTag): ChunkData(timestamp, nbt) {
    var sections: Map<Int, PoiSection>
        get() = nbt["Sections"]!!.getAs<CompoundTag>().value.entries.associate { (k, v) -> k.toInt() to PoiSection(v.getAs()) }
        set(value) {
            nbt["Sections"] = CompoundTag(value.entries.associate { it.key.toString() to it.value.nbt }.toMutableMap(), "Sections", nbt)
        }
    var dataVersion: Int
        get() = nbt["DataVersion"]!!.getAs<IntTag>().value
        set(value) {
            nbt["DataVersion"] = IntTag(value, "DataVersion", nbt)
        }
}

class MutablePoi(override val location: ChunkLocation, timestamp: Int, nbt: CompoundTag): ChunkData(timestamp, nbt) {
    var sections: MutablePoiSections? = null
    var dataVersion: Int? = null

    fun toPoi(): Poi = Poi(location, timestamp, nbt).apply {
        sections = this@MutablePoi.sections!!.toPoiSections()
        dataVersion = this@MutablePoi.dataVersion!!
    }
}

data class MutablePoiSections(val nbt: CompoundTag) {

    private val map = mutableMapOf<Int, MutablePoiSection>()

    operator fun get(y: Int): MutablePoiSection? = map[y]

    operator fun set(y: Int, section: MutablePoiSection) {
        map[y] = section
    }

    fun toPoiSections(): Map<Int, PoiSection> {
        return map.map { it.key to it.value.toPoiSection() }.toMap()
    }

}

data class PoiSection(val nbt: CompoundTag) {
    var valid: Byte
        get() = nbt["Valid"]!!.getAs<ByteTag>().value
        set(value) {
            nbt["Valid"] = ByteTag(value, "Valid", nbt)
        }
    var records: List<PoiRecord>
        get() = nbt["Records"]!!.getAs<ListTag>().value.map { PoiRecord(it.getAs()) }
        set(value) {
            nbt["Records"] = ListTag(TagType.TAG_COMPOUND, value.map { it.nbt }, true, "Records", nbt)
        }
}

data class MutablePoiSection(val nbt: CompoundTag) {
    var valid: Byte? = null
    var records: MutableList<PoiRecord>? = null

    fun toPoiSection(): PoiSection = PoiSection(nbt).apply {
        valid = this@MutablePoiSection.valid!!
        records = this@MutablePoiSection.records!!
    }
}

data class PoiRecord(val nbt: CompoundTag) {
    val pos = nbt["pos"]!!.getAs<IntArrayTag>().value
    var freeTickets: Int
        get() = nbt["free_tickets"]!!.getAs<IntTag>().value
        set(value) {
            nbt["free_tickets"] = IntTag(value, "free_tickets", nbt)
        }
    val type = nbt["type"]!!.getAs<StringTag>().value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoiRecord) return false

        if (pos.contentEquals(other.pos)) return true
        return false
    }

    override fun hashCode(): Int {
        return pos.contentHashCode()
    }
}

/** Entity 타입의 Anvil 파일을 구성하는 Chunk 데이터를 갖는 데이터 구조 */
class Entity(timestamp: Int, nbt: CompoundTag): ChunkData(timestamp, nbt) {
    var position: IntArray
        get() = nbt["Position"]!!.getAs<IntArrayTag>().value
        set(value) {
            nbt["Position"] = IntArrayTag(value, "Position", nbt)
        }

    private val xPos get() = position[0]
    private val zPos get() = position[1]

    override val location: ChunkLocation get() = ChunkLocation(xPos, zPos)

    var entities: List<EntityEach>
        get() = nbt["Entities"]!!.getAs<ListTag>().value.map { EntityEach(it.getAs()) }
        set(value) {
            nbt["Entities"] = ListTag(TagType.TAG_COMPOUND, value.map { it.nbt }, true, "Entities", nbt)
        }

    var dataVersion: Int
        get() = nbt["DataVersion"]!!.getAs<IntTag>().value
        set(value) {
            nbt["DataVersion"] = IntTag(value, "DataVersion", nbt)
        }
}

class MutableEntity(override val location: ChunkLocation, timestamp: Int, nbt: CompoundTag): ChunkData(timestamp, nbt) {
    var position: IntArray? = intArrayOf(location.x, location.z)

    var entities: MutableList<EntityEach>? = null
    var dataVersion: Int? = null

    fun toEntity(): Entity = Entity(timestamp, nbt).apply {
        position = this@MutableEntity.position!!
        entities = this@MutableEntity.entities!!
        dataVersion = this@MutableEntity.dataVersion!!
    }
}

data class EntityEach(val nbt: CompoundTag) {
    val id = nbt["id"]!!.getAs<StringTag>().value
    val uuid = nbt["UUID"]!!.getAs<IntArrayTag>().value
    val brain = nbt["Brain"]?.getAs<CompoundTag>()?.let { EntityBrain(it) }
    val pos = nbt["Pos"]!!.getAs<ListTag>().value.map { it.getAs<DoubleTag>().value }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityEach) return false

        if (uuid.contentEquals(other.uuid)) return true

        return false
    }

    override fun hashCode(): Int {
        return uuid.contentHashCode()
    }

}

data class EntityBrain(private val nbt: CompoundTag) {
    val memories = EntityMemories(nbt["memories"]!!.getAs())
}

data class EntityMemories(private val nbt: CompoundTag) {
    var home: EntityMemoryValue?
        get() = nbt["minecraft:home"]?.getAs<CompoundTag>()?.let { EntityMemoryValue(it) }
        set(value) {
            if (value != null) return
            nbt.value.remove("minecraft:home")
        }
    var jobSite: EntityMemoryValue?
        get() = nbt["minecraft:job_site"]?.getAs<CompoundTag>()?.let { EntityMemoryValue(it) }
        set(value) {
            if (value != null) return
            nbt.value.remove("minecraft:job_site")
        }

    var meetingPoint: EntityMemoryValue?
        get() = nbt["minecraft:meeting_point"]?.getAs<CompoundTag>()?.let { EntityMemoryValue(it) }
        set(value) {
            if (value != null) return
            nbt.value.remove("minecraft:meeting_point")
        }
}

data class EntityMemoryValue(private val nbt: CompoundTag) {
    private val value = nbt["value"]!!.getAs<CompoundTag>()
    val pos = value["pos"]!!.getAs<IntArrayTag>().value
    val dimension = value["dimension"]!!.getAs<StringTag>().value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityMemoryValue) return false

        if (pos.contentEquals(other.pos) && dimension == other.dimension) return true

        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

fun List<Terrain>.findChunk(x: Int, z: Int): Terrain? = find {
    it.nbt.value["xPos"]!!.getAs<IntTag>().value == x && it.nbt["zPos"]!!.getAs<IntTag>().value == z
}
