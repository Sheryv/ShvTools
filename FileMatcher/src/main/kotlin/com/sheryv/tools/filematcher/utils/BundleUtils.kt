package com.sheryv.tools.filematcher.utils

import com.sheryv.tools.filematcher.model.*
import com.sheryv.util.Strings
import java.time.OffsetDateTime

object BundleUtils {
  @JvmStatic
  fun createExample(): Repository {
    val b = listOf(
      Bundle(
        idForBundle(),
        "",
        "Bundle description \nfirst pack ",
        listOf(
          BundleVersion(
            1, "0.2", "0.2", "Fixed item #57",
            listOf(
              Entry(
                idForEntry(),
                "new item 1",
                "",
                "1"
              ), createGroup(
                "new group 2",
                "new group 2"
              ), createGroup(
                "gg group 1",
                "gg group 1",
                parent = "new group 2"
              ), Entry(
                idForEntry(),
                "gg gg item 1",
                "",
                "1",
                parent = "gg group 1"
              ), Entry(
                "testid",
                "gg gg path.jar",
                "",
                "1",
                parent = "gg group 1"
              ), Entry(
                idForEntry(),
                "gg 1",
                "",
                "1",
                parent = "new group 2"
              ), Entry(
                idForEntry(),
                "gg 2",
                "",
                "1",
                parent = "new group 2"
              ), createGroup(
                "third 1",
                "third 1"
              ), createGroup(
                "third 2",
                "third 2",
                parent = "third 1"
              ), Entry(
                idForEntry(),
                "gg gg item 1",
                "",
                "1",
                parent = "third 2"
              ), Entry(
                "testid3",
                "third 3.jar",
                "",
                "1",
                parent = "third 2"
              ), Entry(
                "testid2",
                "second gg path.jar",
                "",
                "1",
                parent = "third 1"
              ), Entry(
                idForEntry(),
                "new item 4",
                "",
                "2"
              )
            )
          ), BundleVersion(
            0, "0.1", "0.1", "Fixed item #5",
            listOf(
              Entry(
                "",
                "item 1",
                "",
                "1"
              ), Entry(
                idForEntry(),
                "item 2",
                "",
                "1"
              )
            )
          )
        )
      ), Bundle(
        idForBundle(),
        "Test 2",
        "Bundle description \nsecond pack ",
        listOf(
          BundleVersion(
            9, "10.2", "10.2", "Total item #57",
            listOf(
              Entry(
                "",
                "new ii 1",
                "",
                "1"
              ), Entry(
                idForEntry(),
                "new ii 2",
                "",
                "1"
              ), Entry(
                idForEntry(),
                "new ii 3",
                "",
                "2"
              )
            )
          ), BundleVersion(
            9, "9.1", "9.1", "Total item #5",
            listOf(
              Entry(
                idForEntry(),
                "ii 1",
                "",
                "1"
              ), Entry(
                idForEntry(),
                "ii 2",
                "",
                "1"
              )
            )
          )
        )
      )
    )
    return Repository(
      "h", "base", "0.123.2", 1, null,
      "Long title askdjnbjkasndjknas nasndjasndkjnskj dsfkjsdn fakjsd skdjn fkjsdnkfj as jskjdfn kskdjfiusdhf kjasdkjf hsakjdf kajsdhf skadhf kljashd fkl",
      "Unknown","description",  mapOf("first" to null, "sec" to "http://google.com"), bundles = b
    )
  }
  
  @JvmStatic
  fun nginxExample(): Repository {
    val bunds = listOf(
      Bundle(
        idForBundle(),
        "Test 1",
        "Bundle description \nfirst pack ",
        listOf(
          BundleVersion(
            2, "0.2", "0.2", "Fixed item #57",
            listOf(
              Entry(
                "aaid",
                "Aroma.jar",
                "Aroma.jar",
                "2"
              ), createGroup(
                "group",
                "group"
              ), Entry(
                "ccid",
                "CoFH.jar",
                "server_dir/CoFH.jar",
                "1",
                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris in magna eget urna " +
                    "pharetra ornare. Suspendisse commodo varius orci vel hendrerit. Cras placerat tempor ex, id " +
                    "convallis tortor volutpat eget. Integer pellentesque quam ac nunc cursus, quis semper sem " +
                    "facilisis. Phasellus fringilla rhoncus sem ut rhoncus. Donec ac sollicitudin nisl, non" +
                    " ultricies magna. Duis pharetra finibus eros vel ultrices. Nam porta tristique lectus et" +
                    " vulputate. Integer tellus odio, imperdiet vitae lacus sed, sagittis tristique metus. " +
                    "Phasellus condimentum in sem ut maximus. Aenean ultricies sollicitudin interdum. ",
                parent = "group",
                additionalFields = mapOf(
                  "f1" to "a", "f2" to "a", "f3" to "a", "f4" to "a", "f5" to "a", "f6" to "a",
                  "f7" to "https://google.com", "f8" to "a", "f9" to "a", "f10" to "a", "f11" to "a", "f12" to "a",
                  "f13" to "a", "f14" to "a", "f15" to "a"
                )
              ), Entry(
                "bbid",
                "Baubles.jar",
                "/Baubles.jar",
                "1",
                hashes = Hash("DF86FF98CC6621D9EAE1DE18A13EE448")
              ), Entry(
                "jjid",
                "Jei.jar",
                "/Jei.jar",
                "1",
                hashes = Hash("69A985FE6640E0EDB47C6C894C579CA2")
              )
            )
          ), BundleVersion(
            1, "0.1", "0.1", "Fixed item #4",
            listOf(
              Entry(
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
              )
            )
          )
        ),
        preferredBasePath = BasePath("D:\\ShvFileMatcher\\target"),
        baseItemUrl = "http://localhost",
        updateDate = Utils.now()
      )
    )
    
    return Repository(
      "http://localhost/", "base", "0.32.2", 1,
      "https://stackoverflow.com/questions/41496752/javafx-node-will-not-grow-in-height-despite-constraints-when-buried-within-sever",
      "Long title", "Unknown","description", mapOf("first" to 123.toString(), "sec" to "http://google.com"), bundles = bunds
    )
  }
  
  fun idForEntry() = Strings.generateId(13)
  
  fun idForBundle() = Strings.generateId(8)
  
  /* fun forEachEntry(root: List<Entry>, callback: (Entry) -> Unit) {
     for (item in root) {
       callback.invoke(item)
       if (item.group) {
         forEachEntry((item as Group).entries, callback)
       }
     }
   }
   */
  fun createGroup(
    id: String,
    name: String,
    parent: String? = null,
    target: TargetPath = TargetPath(BasePath(name)),
    selected: Boolean = true,
    itemDate: OffsetDateTime? = null,
    additionalFields: Map<String, String?> = emptyMap()
  ): Entry {
    return Entry(
      id,
      name,
      "",
      selected = selected,
      target = target,
      parent = parent,
      itemDate = itemDate,
      additionalFields = additionalFields,
      group = true
    )
  }
  
  fun getAllChildren(parentId: String, list: List<Entry>): List<Entry> {
    val result = mutableListOf<Entry>()
    for (entry in list) {
      if (entry.parent == parentId) {
        result.add(entry)
      }
      if (entry.group) {
        result.addAll(getAllChildren(entry.id, list))
      }
    }
    return result
  }
  
  fun getFirstLevelChildren(parentId: String, list: List<Entry>): List<Entry> {
    return list.filter { parentId == it.parent }
  }
  
  fun getParents(parentId: String, entries: List<Entry>): List<Entry> {
    val parents = mutableListOf<Entry>()
    val found = entries.first { it.id == parentId }
    check(found.group) { "Parent have to have 'group=true'$found" }
    parents.add(found)
    
    if (found.parent != null) {
      parents.addAll(getParents(found.parent, entries))
    }
    return parents
  }
  
  fun defaultOptions() = SaveOptions()
  
  fun copyRepo(repo: Repository): Repository {
    return repo.copy(bundles = repo.bundles.map { it.copy() })
  }
}
