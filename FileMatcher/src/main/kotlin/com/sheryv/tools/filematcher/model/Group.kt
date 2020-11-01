package com.sheryv.tools.filematcher.model

class Group(id: String,
            name: String,
            target: TargetPath = TargetPath(BasePath(name)),
            selected: Boolean = false,
            var entries: List<Entry> = emptyList()) :
    Entry(
        id,
        name,
        "",
        type = ItemType.GROUP,
        selected = selected,
        target = target
    ) {
}