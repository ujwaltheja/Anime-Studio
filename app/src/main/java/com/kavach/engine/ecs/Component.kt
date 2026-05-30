package com.kavach.engine.ecs

abstract class Component {
    lateinit var gameObject: GameObject
        internal set

    var enabled: Boolean = true

    open fun onAttach() {}
    open fun onStart() {}
    open fun update(deltaTime: Float) {}
    open fun onDestroy() {}
}
