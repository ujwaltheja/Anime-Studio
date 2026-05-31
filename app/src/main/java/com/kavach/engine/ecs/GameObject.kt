package com.kavach.engine.ecs

class GameObject(val name: String) {
    val transform = Transform()
    var enabled   = true

    private val components = mutableListOf<Component>()

    fun addComponent(component: Component): GameObject {
        component.gameObject = this
        components.add(component)
        component.onAttach()
        return this
    }

    fun removeComponent(component: Component) {
        if (components.remove(component)) component.onDestroy()
    }

    inline fun <reified T : Component> getComponent(): T? =
        components.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : Component> requireComponent(): T =
        getComponent() ?: throw IllegalStateException("${name} missing component ${T::class.simpleName}")

    fun start() = components.forEach { if (it.enabled) it.onStart() }

    fun update(deltaTime: Float) {
        if (!enabled) return
        components.forEach { if (it.enabled) it.update(deltaTime) }
    }

    fun destroy() = components.forEach { it.onDestroy() }
}
