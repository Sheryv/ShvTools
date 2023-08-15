import com.sheryv.util.logging.ConsoleUtils
import com.sheryv.util.logging.log
import com.sheryv.util.logging.systemPrintColored

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    println("&3&Color&0& after " + ConsoleUtils.parseAndReplaceWithColors("&3&Color&0&"))
    for (i in 1..6) {
      log.systemPrintColored(">> &$i&Color&0& and no color")
    }
  }
}
