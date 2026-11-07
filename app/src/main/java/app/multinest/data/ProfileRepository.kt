package app.multinest.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProfileRepository(private val dao: ProfileDao) {
    fun observe(query: String): Flow<List<ProfileEntity>> = dao.observe(query).map { profiles ->
        profiles.sortedWith(profileNameComparator)
    }

    /** Sort numbered profile batches naturally: Qwen 1, Qwen 2, ... Qwen 10. */
    private val profileNameComparator = Comparator<ProfileEntity> { a, b ->
        val numberedName = Regex("^(.*?)(?:\\s+)(\\d+)$")
        val ma = numberedName.matchEntire(a.name.trim())
        val mb = numberedName.matchEntire(b.name.trim())
        if (ma != null && mb != null) {
            val prefixCompare = ma.groupValues[1].compareTo(mb.groupValues[1], ignoreCase = true)
            if (prefixCompare != 0) prefixCompare
            else {
                val numberCompare = ma.groupValues[2].toLongOrNull().orEmptyLong()
                    .compareTo(mb.groupValues[2].toLongOrNull().orEmptyLong())
                if (numberCompare != 0) numberCompare else a.createdAt.compareTo(b.createdAt)
            }
        } else {
            a.name.compareTo(b.name, ignoreCase = true)
        }
    }

    private fun Long?.orEmptyLong(): Long = this ?: 0L
    suspend fun get(id: String) = dao.get(id)
    suspend fun count() = dao.count()

    suspend fun create(name: String, url: String, color: Int, icon: String): ProfileEntity {
        val p = ProfileEntity(
            id = UUID.randomUUID().toString(), name = name, url = url, colorArgb = color,
            iconKey = icon, lastUrl = null, favorite = false, createdAt = System.currentTimeMillis(),
        )
        dao.insert(p)
        return p
    }

    suspend fun createMany(list: List<ProfileEntity>) = dao.insertAll(list)
    suspend fun update(p: ProfileEntity) = dao.update(p)
    suspend fun setLastUrl(id: String, url: String) = dao.setLastUrl(id, url)
    suspend fun setStartUrl(id: String, url: String) = dao.setStartUrl(id, url)
    suspend fun delete(id: String) = dao.delete(id)
}
