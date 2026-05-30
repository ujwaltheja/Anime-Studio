package com.kavach.engine.assets.model

import android.content.Context
import android.util.Log
import com.kavach.engine.assets.AssetLoader
import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.Mesh
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads binary glTF 2.0 (.glb) files.
 *
 * Supports: positions, normals, UV0, unsigned-int / unsigned-short indices.
 * Does NOT yet support: animations, skinning, sparse accessors, external buffers.
 */
object GLBLoader {

    private const val TAG = "GLBLoader"

    // glTF component types
    private const val CT_UBYTE  = 5121
    private const val CT_USHORT = 5123
    private const val CT_UINT   = 5125
    private const val CT_FLOAT  = 5126

    fun load(context: Context, assetPath: String): LoadedModel? {
        return try {
            val bytes = AssetLoader.loadBytes(context, assetPath)
            parse(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $assetPath: ${e.message}")
            null
        }
    }

    private fun parse(data: ByteArray): LoadedModel? {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        // ── Header ─────────────────────────────────────────────────────────
        val magic   = buf.int           // 0x46546C67 = "glTF"
        val version = buf.int
        /*val total =*/ buf.int

        if (magic != 0x46546C67) { Log.e(TAG, "Not a GLB file"); return null }
        if (version != 2)        { Log.e(TAG, "Only GLB v2 supported"); return null }

        // ── JSON chunk ─────────────────────────────────────────────────────
        val jsonLen  = buf.int
        val jsonType = buf.int          // 0x4E4F534A = "JSON"
        if (jsonType != 0x4E4F534A) { Log.e(TAG, "Expected JSON chunk"); return null }

        val jsonBytes = ByteArray(jsonLen)
        buf.get(jsonBytes)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        // Pad to 4-byte boundary
        repeat((4 - jsonLen % 4) % 4) { buf.get() }

        // ── Binary chunk (optional) ────────────────────────────────────────
        var binBuffer: ByteArray? = null
        if (buf.remaining() >= 8) {
            val binLen  = buf.int
            val binType = buf.int       // 0x004E4942 = "BIN\0"
            if (binType == 0x004E4942) {
                binBuffer = ByteArray(binLen)
                buf.get(binBuffer)
            }
        }

        val materials = parseMaterials(json)
        val meshes    = parseMeshes(json, binBuffer)
        return LoadedModel(meshes, materials)
    }

    // ── Materials ──────────────────────────────────────────────────────────

    private fun parseMaterials(json: JSONObject): List<MaterialData> {
        val jArr = json.optJSONArray("materials") ?: return listOf(MaterialData())
        return (0 until jArr.length()).map { i ->
            val m   = jArr.getJSONObject(i)
            val pbr = m.optJSONObject("pbrMetallicRoughness")
            val cf  = pbr?.optJSONArray("baseColorFactor")
            MaterialData(
                name      = m.optString("name", "Material$i"),
                baseColor = if (cf != null) Vector3(cf.getDouble(0).toFloat(),
                                                    cf.getDouble(1).toFloat(),
                                                    cf.getDouble(2).toFloat())
                            else Vector3(1f, 1f, 1f),
                metallic  = pbr?.optDouble("metallicFactor",  0.0)?.toFloat() ?: 0f,
                roughness = pbr?.optDouble("roughnessFactor", 0.5)?.toFloat() ?: 0.5f
            )
        }
    }

    // ── Meshes ─────────────────────────────────────────────────────────────

    private fun parseMeshes(json: JSONObject, bin: ByteArray?): List<Mesh> {
        if (bin == null) return emptyList()

        val accessors   = json.optJSONArray("accessors")   ?: return emptyList()
        val bufferViews = json.optJSONArray("bufferViews") ?: return emptyList()
        val jMeshes     = json.optJSONArray("meshes")      ?: return emptyList()

        val result = mutableListOf<Mesh>()

        for (mi in 0 until jMeshes.length()) {
            val jMesh = jMeshes.getJSONObject(mi)
            val prims = jMesh.optJSONArray("primitives") ?: continue

            for (pi in 0 until prims.length()) {
                val prim  = prims.getJSONObject(pi)
                val attrs = prim.optJSONObject("attributes") ?: continue

                val posIdx  = attrs.optInt("POSITION",    -1)
                val normIdx = attrs.optInt("NORMAL",      -1)
                val uvIdx   = attrs.optInt("TEXCOORD_0",  -1)
                val idxIdx  = prim.optInt("indices",      -1)

                if (posIdx < 0) continue

                val positions = readFloatAccessor(accessors, bufferViews, bin, posIdx)
                val normals   = if (normIdx >= 0) readFloatAccessor(accessors, bufferViews, bin, normIdx)
                                else syntheticNormals(positions)
                val uvs       = if (uvIdx >= 0) readFloatAccessor(accessors, bufferViews, bin, uvIdx)
                                else FloatArray(positions.size / 3 * 2)
                val indices   = if (idxIdx >= 0) readIndices(accessors, bufferViews, bin, idxIdx)
                                else IntArray(positions.size / 3) { it }

                result.add(interleave(positions, normals, uvs, indices))
            }
        }
        return result
    }

    private fun interleave(
        pos: FloatArray, norm: FloatArray, uv: FloatArray, indices: IntArray
    ): Mesh {
        val vertexCount = pos.size / 3
        val verts = FloatArray(vertexCount * 8)
        for (v in 0 until vertexCount) {
            verts[v * 8 + 0] = pos[v * 3 + 0]
            verts[v * 8 + 1] = pos[v * 3 + 1]
            verts[v * 8 + 2] = pos[v * 3 + 2]
            verts[v * 8 + 3] = if (v * 3 + 2 < norm.size) norm[v * 3 + 0] else 0f
            verts[v * 8 + 4] = if (v * 3 + 2 < norm.size) norm[v * 3 + 1] else 1f
            verts[v * 8 + 5] = if (v * 3 + 2 < norm.size) norm[v * 3 + 2] else 0f
            verts[v * 8 + 6] = if (v * 2 + 1 < uv.size)   uv[v * 2 + 0]   else 0f
            verts[v * 8 + 7] = if (v * 2 + 1 < uv.size)   uv[v * 2 + 1]   else 0f
        }
        return Mesh(verts, indices)
    }

    // ── Accessor readers ───────────────────────────────────────────────────

    private fun readFloatAccessor(
        accessors: JSONArray, bufferViews: JSONArray, bin: ByteArray, idx: Int
    ): FloatArray {
        val acc        = accessors.getJSONObject(idx)
        val count      = acc.getInt("count")
        val compType   = acc.getInt("componentType")
        val typeStr    = acc.getString("type")
        val components = typeComponents(typeStr)
        val accOffset  = acc.optInt("byteOffset", 0)

        val bvIdx      = acc.getInt("bufferView")
        val bv         = bufferViews.getJSONObject(bvIdx)
        val bvOffset   = bv.optInt("byteOffset", 0)
        val byteStride = bv.optInt("byteStride", 0)
        val elemSize   = components * componentSize(compType)
        val stride     = if (byteStride > 0) byteStride else elemSize
        val base       = bvOffset + accOffset

        val out = FloatArray(count * components)
        for (i in 0 until count) {
            val off = base + i * stride
            val eb  = ByteBuffer.wrap(bin, off, elemSize).order(ByteOrder.LITTLE_ENDIAN)
            for (c in 0 until components) {
                out[i * components + c] = readComponent(eb, compType)
            }
        }
        return out
    }

    private fun readIndices(
        accessors: JSONArray, bufferViews: JSONArray, bin: ByteArray, idx: Int
    ): IntArray {
        val acc       = accessors.getJSONObject(idx)
        val count     = acc.getInt("count")
        val compType  = acc.getInt("componentType")
        val accOffset = acc.optInt("byteOffset", 0)

        val bv        = bufferViews.getJSONObject(acc.getInt("bufferView"))
        val bvOffset  = bv.optInt("byteOffset", 0)
        val compSz    = componentSize(compType)
        val base      = bvOffset + accOffset

        return IntArray(count) { i ->
            val eb = ByteBuffer.wrap(bin, base + i * compSz, compSz).order(ByteOrder.LITTLE_ENDIAN)
            when (compType) {
                CT_UINT   -> eb.int
                CT_USHORT -> eb.short.toInt() and 0xFFFF
                CT_UBYTE  -> eb.get().toInt()  and 0xFF
                else      -> eb.int
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun readComponent(buf: ByteBuffer, compType: Int): Float = when (compType) {
        CT_FLOAT  -> buf.float
        CT_USHORT -> (buf.short.toInt() and 0xFFFF).toFloat()
        CT_UBYTE  -> (buf.get().toInt()  and 0xFF).toFloat()
        else      -> buf.float
    }

    private fun typeComponents(type: String) = when (type) {
        "SCALAR" -> 1; "VEC2" -> 2; "VEC3" -> 3; "VEC4" -> 4; "MAT4" -> 16; else -> 1
    }

    private fun componentSize(type: Int) = when (type) {
        CT_UBYTE         -> 1
        CT_USHORT        -> 2
        CT_UINT, CT_FLOAT -> 4
        else             -> 4
    }

    private fun syntheticNormals(positions: FloatArray): FloatArray =
        FloatArray(positions.size).also { n ->
            for (i in n.indices step 3) { n[i + 1] = 1f }
        }
}
