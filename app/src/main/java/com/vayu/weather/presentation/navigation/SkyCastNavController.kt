package com.vayu.weather.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * A small, saveable navigation controller for the SkyCast app.
 *
 * The app uses Compose state-based navigation rather than Navigation 3
 * because the route graph is small, single-source-of-truth, and we need
 * predictable back behavior for modal dialogs.
 *
 * Features:
 *  - Push / pop stack semantics
 *  - Saveable across process recreation ([primaryRoute], [stack])
 *  - Type-safe [Routes] rather than String
 *  - Convenience [navigate] for primary destinations (single-level)
 */
class SkyCastNavController(
    initial: Routes = Routes.default
) {
    var primaryRoute: Routes by mutableStateOf(initial)

    private val _stack: SnapshotStateList<Routes> = mutableStateListOf()

    /** Secondary screens pushed on top of the current primary. */
    val stack: List<Routes> get() = _stack

    val currentRoute: Routes
        get() = _stack.lastOrNull() ?: primaryRoute

    fun navigate(route: Routes) {
        if (route in Routes.primaryDestinations) {
            primaryRoute = route
            _stack.clear()
        } else {
            _stack.add(route)
        }
    }

    fun popBackStack(): Boolean {
        if (_stack.isNotEmpty()) {
            _stack.removeAt(_stack.lastIndex)
            return true
        }
        return false
    }

    fun popUntilPrimary() {
        _stack.clear()
    }

    fun replacePrimary(route: Routes) {
        primaryRoute = route
        _stack.clear()
    }

    /** Internal hook used by the saveable wrapper. */
    fun restoreStack(snapshot: List<Routes>) {
        _stack.clear()
        _stack.addAll(snapshot)
    }

    fun snapshotStack(): List<Routes> = _stack.toList()

    companion object {
        /**
         * Saver that persists the controller's primary route + secondary
         * stack across process death.
         */
        fun saver(): Saver<SkyCastNavController, Any> = Saver(
            save = {
                listOf(
                    it.primaryRoute.route
                ) + it.snapshotStack().map { route -> route.route }
            },
            restore = { raw ->
                @Suppress("UNCHECKED_CAST")
                val list = raw as List<String>
                val controller = SkyCastNavController(
                    initial = Routes.fromRouteString(list.firstOrNull()) ?: Routes.default
                )
                if (list.size > 1) {
                    controller.restoreStack(list.drop(1).mapNotNull { Routes.fromRouteString(it) })
                }
                controller
            }
        )
    }
}

@Composable
fun rememberSkyCastNavController(
    initial: Routes = Routes.default
): SkyCastNavController {
    return rememberSaveable(saver = SkyCastNavController.saver()) {
        SkyCastNavController(initial)
    }
}
