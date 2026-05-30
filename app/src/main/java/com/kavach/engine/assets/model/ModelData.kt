package com.kavach.engine.assets.model

import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.Mesh

/** Result of loading a GLB/glTF file. */
data class LoadedModel(
    val meshes: List<Mesh>,
    val materials: List<MaterialData>
) {
    /** The first mesh, or a fallback empty mesh. Convenient for single-mesh models. */
    val primaryMesh: Mesh get() = meshes.firstOrNull() ?: Mesh(FloatArray(0), IntArray(0))
    val primaryMaterial: MaterialData get() = materials.firstOrNull() ?: MaterialData()
}

data class MaterialData(
    val baseColor: Vector3 = Vector3(1f, 1f, 1f),
    val metallic: Float   = 0f,
    val roughness: Float  = 0.5f,
    val name: String      = ""
)
