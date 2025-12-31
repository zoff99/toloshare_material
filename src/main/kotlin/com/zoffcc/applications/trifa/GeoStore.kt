@file:Suppress("PropertyName", "LocalVariableName", "FunctionName")

package com.zoffcc.applications.trifa

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import kotlin.collections.forEach
import kotlin.collections.plus

data class StateGeoLocations(val remote_locations: List<GeoItem> = emptyList(), val follow_pk: String? = null) // , var osm: OsmViewModel? = null)

data class GeoItem(
    val name: String,
    val pk_str: String,
    val lat: Double,
    val lon: Double,
    val acc: Float,
    val last_remote_location_ts_millis: Long = 0,
) {
    fun updateName(n: String) = copy(name = n)
}

interface GeoStore
{
    fun add(item: GeoItem)
    fun update(item: GeoItem)
    fun setFollowPk(pk: String?)
    fun getFollowPk(): String?
    fun clear()
    val stateFlow: StateFlow<StateGeoLocations>
    val state get() = stateFlow.value
}

fun CoroutineScope.createGeoStore(): GeoStore
{
    val mutableStateFlow = MutableStateFlow(StateGeoLocations())

    return object : GeoStore
    {
        override val stateFlow: StateFlow<StateGeoLocations> = mutableStateFlow

        override fun add(item: GeoItem)
        {
            mutableStateFlow.value = state.copy(remote_locations = state.remote_locations + item)
        }

        override fun update(item: GeoItem)
        {
            var update_item: GeoItem? = null
            state.remote_locations.forEach {
                if (item.pk_str == it.pk_str)
                {
                    update_item = it.copy()
                }
            }
            if (update_item != null)
            {
                val new_remote_locations: ArrayList<GeoItem> = ArrayList()
                new_remote_locations.addAll(state.remote_locations)
                var to_remove_item: GeoItem? = null
                new_remote_locations.forEach { item2 ->
                    if (item2.pk_str == update_item!!.pk_str)
                    {
                        to_remove_item = item2
                    }
                }
                if (to_remove_item != null)
                {
                    new_remote_locations.remove(to_remove_item)
                }
                new_remote_locations.add(item)
                mutableStateFlow.value = state.copy(remote_locations = new_remote_locations)
            } else
            {
                mutableStateFlow.value = state.copy(remote_locations = state.remote_locations + item)
            }
        }
        override fun setFollowPk(pk: String?)
        {
            mutableStateFlow.value = state.copy(follow_pk = pk)
        }
        override fun getFollowPk(): String?
        {
            return state.follow_pk
        }

        override fun clear()
        {
            mutableStateFlow.value = state.copy(remote_locations = emptyList())
        }

    }
}

