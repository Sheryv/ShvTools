package com.sheryv.tools.filematcher.utils

import com.sheryv.tools.filematcher.model.*
import com.sheryv.util.Strings
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn

object BundleUtils {
  @JvmStatic
  fun createExample(): Repository {
    val b = listOf(Bundle(
        Strings.generateId(2),
        "",
        "Bundle description \nfirst pack ",
        listOf(BundleVersion(1, "0.2", "Fixed item #57",
            listOf(Entry(
                Strings.generateId(4),
                "new item 1",
                "",
                "1"
            ), Group(
                Strings.generateId(4),
                "new group 2",
                entries = listOf(Entry(
                    Strings.generateId(4),
                    "gg 1",
                    "",
                    "1"
                ), Entry(
                    Strings.generateId(4),
                    "gg 2",
                    "",
                    "1"
                ), Group(
                    Strings.generateId(4),
                    "gg group 1",
                    entries = listOf(Entry(
                        Strings.generateId(4),
                        "gg gg item 1",
                        "",
                        "1"
                    ), Entry(
                        "testid",
                        "gg gg path.jar",
                        "",
                        "1"
                    ))
                ))
            ), Group(
                Strings.generateId(4),
                "third 1",
                entries = listOf(Group(
                    Strings.generateId(4),
                    "third 2",
                    entries = listOf(Entry(
                        Strings.generateId(4),
                        "gg gg item 1",
                        "",
                        "1"
                    ), Entry(
                        "testid3",
                        "third 3.jar",
                        "",
                        "1"
                    ))
                ), Entry(
                    "testid2",
                    "second gg path.jar",
                    "",
                    "1"
                ))
            ), Entry(
                Strings.generateId(4),
                "new item 4",
                "",
                "2"
            ))
        ), BundleVersion(0, "0.1", "Fixed item #5",
            listOf(Entry(
                "",
                "item 1",
                "",
                "1"
            ), Entry(
                Strings.generateId(4),
                "item 2",
                "",
                "1"
            ))
        ))
    ), Bundle(
        Strings.generateId(2),
        "Test 2",
        "Bundle description \nsecond pack ",
        listOf(BundleVersion(9, "10.2", "Total item #57",
            listOf(Entry(
                "",
                "new ii 1",
                "",
                "1"
            ), Entry(
                Strings.generateId(4),
                "new ii 2",
                "",
                "1"
            ), Entry(
                Strings.generateId(4),
                "new ii 3",
                "",
                "2"
            ))
        ), BundleVersion(9, "9.1", "Total item #5",
            listOf(Entry(
                Strings.generateId(4),
                "ii 1",
                "",
                "1"
            ), Entry(
                Strings.generateId(4),
                "ii 2",
                "",
                "1"
            ))
        ))
    ))
    return Repository("h", "base", "0.123.2", null,
        "Long title askdjnbjkasndjknas nasndjasndkjnskj dsfkjsdn fakjsd skdjn fkjsdnkfj as jskjdfn kskdjfiusdhf kjasdkjf hsakjdf kajsdhf skadhf kljashd fkl",
        "Unknown", mapOf("first" to null, "sec" to "http://google.com"), bundles = b)
  }
  
  @JvmStatic
  fun nginxExample(): Repository {
    val bunds = listOf(Bundle(
        Strings.generateId(2),
        "Test 1",
        "Bundle description \nfirst pack ",
        listOf(BundleVersion(2, "0.2", "Fixed item #57",
            listOf(Entry(
                "aaid",
                "Aroma.jar",
                "Aroma.jar",
                "2"
            ), Group(
                Strings.generateId(4),
                "group",
                entries = listOf(Entry(
                    "ccid",
                    "CoFH.jar",
                    "/CoFH.jar",
                    "1"
                ))
            ))
        ), BundleVersion(1, "0.1", "Fixed item #4",
            listOf(Entry(
                "wwid",
                "Waystones.jar",
                "Waystones.jar",
                "1",
                target = TargetPath(override = false)
            ), Entry(
                "aaid",
                "Aroma.jar",
                "Aroma1.jar",
                "1"
            ))
        )),
        preferredBasePath = BasePath("D:\\ShvFileMatcher\\target"),
        baseItemUrl = "http://localhost"
    ))
    
    return Repository("http://localhost/", "base", "0.32.2",
        "https://stackoverflow.com/questions/41496752/javafx-node-will-not-grow-in-height-despite-constraints-when-buried-within-sever",
        "Long title", "Unknown", mapOf("first" to 123.toString(), "sec" to "http://google.com"), bundles = bunds)
  }
  
  
  fun forEachEntry(root: List<Entry>, callback: (Entry) -> Unit) {
    for (item in root) {
      callback.invoke(item)
      if (item.isGroup()) {
        forEachEntry((item as Group).entries, callback)
      }
    }
  }
}