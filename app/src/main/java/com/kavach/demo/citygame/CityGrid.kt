package com.kavach.demo.citygame

import com.kavach.engine.ecs.GameObject
import org.json.JSONArray
import org.json.JSONObject

/** Manages the logical state of the city's NxN tile grid. */
class CityGrid(val size: Int = 20) {

    private val cells = Array(size) { arrayOfNulls<BuildingType>(size) }
    private val objects = HashMap<Long, GameObject>(size * size / 2)

    fun isValid(x: Int, z: Int) = x in 0 until size && z in 0 until size
    fun isEmpty(x: Int, z: Int) = isValid(x, z) && cells[x][z] == null
    fun getBuilding(x: Int, z: Int): BuildingType? = if (isValid(x, z)) cells[x][z] else null

    fun place(x: Int, z: Int, type: BuildingType, obj: GameObject) {
        cells[x][z] = type
        objects[key(x, z)] = obj
    }

    fun removeObject(x: Int, z: Int): GameObject? {
        cells[x][z] = null
        return objects.remove(key(x, z))
    }

    fun getObject(x: Int, z: Int) = objects[key(x, z)]

    fun allPlacements(): List<Triple<Int, Int, BuildingType>> {
        val result = mutableListOf<Triple<Int, Int, BuildingType>>()
        for (x in 0 until size) for (z in 0 until size) {
            cells[x][z]?.let { result.add(Triple(x, z, it)) }
        }
        return result
    }

    fun serialize(): String {
        val arr = JSONArray()
        for (x in 0 until size) {
            val row = JSONArray()
            for (z in 0 until size) row.put(cells[x][z]?.name ?: "")
            arr.put(row)
        }
        return JSONObject().put("grid", arr).toString()
    }

    fun deserialize(json: String) {
        val arr = JSONObject(json).getJSONArray("grid")
        for (x in 0 until minOf(size, arr.length())) {
            val row = arr.getJSONArray(x)
            for (z in 0 until minOf(size, row.length())) {
                val s = row.getString(z)
                cells[x][z] = if (s.isEmpty()) null else runCatching { BuildingType.valueOf(s) }.getOrNull()
            }
        }
    }

    fun clear() {
        for (x in 0 until size) cells[x].fill(null)
        objects.clear()
    }

    private fun key(x: Int, z: Int) = x.toLong() shl 16 or z.toLong()
}
