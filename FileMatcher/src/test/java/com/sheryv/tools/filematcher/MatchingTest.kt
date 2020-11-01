package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.UserContext
import com.sheryv.tools.filematcher.utils.BundleUtils
import org.junit.jupiter.api.Test
import java.io.File

class MatchingTest {
  @Test
  fun name() {
    
    val example = BundleUtils.createExample()
    val context = UserContext(example, example.bundles[0].id, example.bundles[0].versions[0].versionId, File("C:\\temp"))
    
    val path = context.buildDirPathForEntry(Entry("testid3", "", ""))
    println()
  }
}
