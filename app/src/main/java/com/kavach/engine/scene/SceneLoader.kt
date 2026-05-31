package com.kavach.engine.scene

import android.content.Context
import com.kavach.engine.assets.AssetLoader
import com.kavach.engine.ecs.GameObject
import com.kavach.engine.ecs.Scene
import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.MeshRenderer
import com.kavach.engine.renderer.Shader
import com.kavach.engine.renderer.primitives.CubeMesh
import com.kavach.engine.renderer.primitives.PlaneMesh
import com.kavach.engine.renderer.primitives.SphereMesh
import org.json.JSONObject

/**
 * Loads a [Scene] from a JSON file inside assets/levels/.
 *
 * JSON format:
 * ```json
 * {
 *   "name": "MyScene",
 *   "camera": { "position": [x,y,z], "target": [x,y,z], "fov": 45 },
 *   "light": { "direction": [x,y,z], "color": [r,g,b], "ambient": [r,g,b] },
 *   "objects": [
 *     { "name": "Ground", "mesh": "plane",
 *       "position": [0,0,0], "rotation": [0,0,0], "scale": [10,1,10],
 *       "color": [r,g,b] }
 *   ]
 * }
 * ```
 * Built-in mesh names: "cube", "plane", "sphere".
 */
object SceneLoader {

    fun load(context: Context, assetPath: String, vertSrc: String, fragSrc: String): Scene {
        val jsonStr = AssetLoader.loadText(context, assetPath)
        return parse(context, jsonStr, vertSrc, fragSrc)
    }

    private fun parse(context: Context, jsonStr: String, vertSrc: String, fragSrc: String): Scene {
        val root = JSONObject(jsonStr)
        val scene = Scene(root.optString("name", "Scene"))

        // Camera
        root.optJSONObject("camera")?.let { cam ->
            cam.optJSONArray("position")?.let { a ->
                scene.camera.position.set(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
            }
            cam.optJSONArray("target")?.let { a ->
                scene.camera.target.set(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
            }
            scene.camera.fov = cam.optDouble("fov", 45.0).toFloat()
        }

        // Light
        root.optJSONObject("light")?.let { l ->
            l.optJSONArray("direction")?.let { a ->
                scene.lightDir.set(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
            }
            l.optJSONArray("color")?.let { a ->
                scene.lightColor.set(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
            }
            l.optJSONArray("ambient")?.let { a ->
                scene.ambientColor.set(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
            }
        }

        // Objects
        val objects = root.optJSONArray("objects") ?: return scene
        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)
            val name = obj.optString("name", "Object$i")
            val meshType = obj.optString("mesh", "cube")

            val mesh = when (meshType) {
                "plane"  -> PlaneMesh.create()
                "sphere" -> SphereMesh.create()
                else     -> CubeMesh.create()
            }.also { it.upload() }

            val shader = Shader(vertSrc, fragSrc).also { it.compile() }
            val mr = MeshRenderer(mesh, shader)

            obj.optJSONArray("color")?.let { c ->
                mr.color = Vector3(c.getDouble(0).toFloat(), c.getDouble(1).toFloat(), c.getDouble(2).toFloat())
            }

            val go = GameObject(name).apply {
                obj.optJSONArray("position")?.let { a ->
                    transform.setPosition(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
                }
                obj.optJSONArray("rotation")?.let { a ->
                    transform.setRotation(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
                }
                obj.optJSONArray("scale")?.let { a ->
                    transform.setScale(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
                }
                addComponent(mr)
            }
            scene.add(go)
        }
        return scene
    }
}
