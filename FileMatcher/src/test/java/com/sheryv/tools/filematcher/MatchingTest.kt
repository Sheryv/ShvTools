package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.Matching
import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.UserContext
import com.sheryv.tools.filematcher.utils.BundleUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class MatchingTest {
  @Test
  @Disabled
  fun name() {
    
    val example = BundleUtils.createExample()
    val context =
      UserContext(example, example.bundles[0].id, example.bundles[0].versions[0].versionId, File("C:\\temp"))
    
    val path = context.buildDirPathForEntry(Entry("testid3", "", ""))
    println()
  }
  
  @Test
  fun wildcard() {
    val input = "b.b+?d*"
    val m = Matching(wildcard = input)
    
    assertFalse(m.matches("""b2b dasd""")) //f
    assertTrue(m.matches(input)) //t
    assertTrue(m.matches("""b.b+ąd""")) //t
    assertTrue(m.matches("""b.b+ d zxc""")) //t
    assertFalse(m.matches("""b2b+1d zxc""")) //f
    assertFalse(m.matches("""b.bbb1d zxc""")) //f
    println()
  }
  
  @Test
  fun regex() {
    val input = "b.b+.?d.*"
    val m = Matching(regex = input)
    
    assertTrue(m.matches("""b2b dasd"""))
    assertFalse(m.matches(input))
    assertFalse(m.matches("""b.b+ąd"""))
    assertFalse(m.matches("""b.b+ d zxc"""))
    assertFalse(m.matches("""b2b+1d zxc"""))
    assertTrue(m.matches("""b.bbb1d zxc"""))
  }
  
  @Test
  fun prefix() {
    val input = "b.b+?d*"
    val m = Matching(prefix = input)
    
    assertFalse(m.matches("""b2b dasd"""))
    assertTrue(m.matches(input))
    assertFalse(m.matches("""b.b+ąd"""))
    assertFalse(m.matches("""b.b+ d zxc"""))
    assertTrue(m.matches("""b.b+?d*d zxc"""))
    assertFalse(m.matches("""b.bbb1d zxc"""))
  }
}
