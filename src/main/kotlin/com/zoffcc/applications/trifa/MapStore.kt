@file:Suppress("FunctionName", "SpellCheckingInspection", "LocalVariableName")

package com.zoffcc.applications.trifa

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.asSource
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream

data class StateMap(val mapdummy: String? = null)

const val StateMapTAG = "trifa.MapStore"

interface MapStore
{
    val state2: MapState
    val stateFlow: StateFlow<StateMap>
    val state get() = stateFlow.value
}

val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
    FileInputStream(File("path/{$zoomLvl}/{$row}/{$col}.jpg")).asSource() // or it can be a remote HTTP fetch
}

val state3 = MapState(4, 4096, 4096, workerCount = 2).apply {
    addLayer(tileStreamProvider)
    // enableRotation()
}

fun CoroutineScope.createMapStore(): MapStore
{
    val mutableStateFlow = MutableStateFlow(StateMap())

    return object : MapStore
    {
        override val state2: MapState
            get() = state3
        override val stateFlow: StateFlow<StateMap> = mutableStateFlow
    }
}

