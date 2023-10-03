package com.reco1l.framework.net.request

import org.json.JSONArray
import org.json.JSONObject

fun interface DataValidator
{
    fun onResponse(jsonObject: JSONObject?, jsonArray: JSONArray?)
}
