@file:Suppress("PropertyName", "LocalVariableName", "FunctionName", "LiftReturnOrAssignment")

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
    val bearing: Float,
    val has_bearing: Boolean = true,
    var prev_lat: Double = -1.0,
    var prev_lon: Double = -1.0,
    var prev_bearing: Float = -1f,
    var prev_has_bearing: Boolean = false,
    val acc: Float,
    val last_remote_location_ts_millis: Long = 0,
    var prev_last_remote_location_ts_millis: Long = 0,
    val direct: Boolean = true,
    val provider: String = "unknown",
) {
    fun updateName(n: String) = copy(name = n)
}

interface GeoStore
{
    // fun add(item: GeoItem)
    fun update(item: GeoItem)
    fun get(pk: String): GeoItem?
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

        //override fun add(item: GeoItem)
        //{
        //    mutableStateFlow.value = state.copy(remote_locations = state.remote_locations + item)
        //}

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
                    if (item.direct)
                    {
                        item.prev_has_bearing = item.has_bearing
                        item.prev_bearing = item.bearing
                        item.prev_lat = item.lat
                        item.prev_lon = item.lon
                        item.prev_last_remote_location_ts_millis = item.prev_last_remote_location_ts_millis
                    }
                    else
                    {
                        item.prev_has_bearing = to_remove_item.has_bearing
                        item.prev_bearing = to_remove_item.bearing
                        item.prev_lat = to_remove_item.lat
                        item.prev_lon = to_remove_item.lon
                        item.prev_last_remote_location_ts_millis = to_remove_item.prev_last_remote_location_ts_millis
                    }
                    new_remote_locations.remove(to_remove_item)
                }
                new_remote_locations.add(item)
                mutableStateFlow.value = state.copy(remote_locations = new_remote_locations)
            } else
            {
                if (item.direct)
                {
                    item.prev_has_bearing = item.has_bearing
                    item.prev_bearing = item.bearing
                    item.prev_lat = item.lat
                    item.prev_lon = item.lon
                    item.prev_last_remote_location_ts_millis = item.prev_last_remote_location_ts_millis
                }
                mutableStateFlow.value = state.copy(remote_locations = state.remote_locations + item)
            }
        }

        override fun get(pk: String): GeoItem?
        {
            var update_item: GeoItem? = null
            state.remote_locations.forEach {
                if (pk == it.pk_str)
                {
                    update_item = it.copy()
                }
            }

            if (update_item != null)
            {
                return update_item
            }
            else
            {
                return null
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
            mutableStateFlow.value = state.copy(remote_locations = emptyList(), follow_pk = null)
        }

    }
}

