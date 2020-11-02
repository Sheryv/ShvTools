package com.sheryv.tools.filematcher.model.minecraft

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class InstanceConfig(
    val installedModpack: Addon,
    val name: String,
    val gameVersion: String,
    val installedAddons: List<Addon>
) {
}