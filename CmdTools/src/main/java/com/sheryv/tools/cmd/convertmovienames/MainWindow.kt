package com.sheryv.tools.cmd.convertmovienames

import com.sheryv.tools.cmd.convertmovienames.videosearch.TmdbApi
import com.sheryv.tools.cmd.convertmovienames.videosearch.SearchItem
import org.apache.commons.lang3.StringUtils
import java.awt.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

private val SUBS = listOf("txt", "srt", "vtt")
private val FILE_NAME_FORBIDDEN_CHARS_PATTER = Regex("[\\\\/:*?\"<>|]")

class MainWindow(private val config: Config, private val results: Map<Path, List<SearchItem>>, private val rootDir: Path) :
  JDialog(null as JDialog?, "Convert movie names results", true) {
  private val font: Font
  
  init {
    font =
      GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts.let {
        it.firstOrNull { it.fontName.equals("consolas", true) }?.let {
          Font("Consolas", Font.PLAIN, 12)
        }
          ?: Font(Font.MONOSPACED, Font.PLAIN, 12)
      }
    defaultCloseOperation = DISPOSE_ON_CLOSE
  }
  
  fun associate(): Map<Path, SearchItem> {
    contentPane = root()
    setSize(1000, 600)
    isVisible = true
    
    return readGui()
  }
  
  private fun readGui() = ((contentPane.components.first() as JViewport).components.first() as JPanel).components
    .filterIsInstance<JPanel>()
    .drop(1)
    .mapNotNull {
      it.components
        .let { row ->
          rootDir.resolve((row.first { it is JLabel } as JLabel).text) to (row.first { it is JComboBox<*> } as JComboBox<SearchItem>).selectedItem as? SearchItem
        }
        .takeIf { it.second != null }
        ?.let { it.first to it.second!! }
    }.toMap()
  
  private fun convertPaths(): List<CalcPaths> {
    return readGui().map { (file, item) ->
      var name = StringUtils.replace(item.name(), " : ", " - ")
      name = StringUtils.replace(name, " :", " - ")
      name = StringUtils.replace(name, ": ", " - ")
      name = FILE_NAME_FORBIDDEN_CHARS_PATTER.replace(StringUtils.replace(name, ":", "-"), "")
      
      val target = rootDir.toAbsolutePath()
        .resolve("$name (${item.date()!!.year})")
        .resolve(name + "." + file.extension)
      
      CalcPaths(file, target, item, rootDir)
    }
  }
  
  
  private fun createNfo(item: SearchItem): String {
    return """
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <movie>
        <title>${item.name()}</title>
        <originaltitle>${item.name()}</originaltitle>
        <year>${item.date()?.year ?: ""}</year>
        <tmdbid>${item.id}</tmdbid>
      </movie>
    """.trimIndent()
  }
  
  
  private fun root(): JComponent {
    
    val p = JPanel()
    p.layout = BoxLayout(p, BoxLayout.Y_AXIS)
    p.border = EmptyBorder(10, 10, 10, 10)
    val btns = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
//    btns.border = EmptyBorder(10, 10, 10, 10)
    btns.add(JButton("Print paths").also {
      it.addActionListener {
        convertPaths().forEach(::println)
      }
    })
    btns.add(JButton("Run").also {
      it.addActionListener {
        convertPaths().forEach { p ->
          Files.createDirectories(p.target.parent)
          Files.move(p.input, p.target)
          Files.writeString(p.nfoPath, createNfo(p.item))
          p.subtitles.forEach { (input, target) ->
            Files.move(input, target)
          }
          println("Done ${p.item.name()}")
        }
      }
    })
    p.add(btns)
    p.add(JSeparator())
    results.forEach { (path, items) ->
      val row = JPanel(BorderLayout(10, 0))
      row.maximumSize = Dimension(row.maximumSize.width, 30)
      val label = JLabel(path.toString())
      label.font = font
      row.add(label, BorderLayout.WEST)
      val combo = JComboBox(items.sortedByDescending { it.popularity }.toTypedArray())
      if (items.isNotEmpty()) {
        combo.selectedIndex = 0
      }
      combo.font = font
      (combo.renderer as JLabel).border = EmptyBorder(5, 0, 0, 0)
      row.add(combo, BorderLayout.CENTER)
      row.add(JButton("Search").also {
        it.addActionListener {
          val phrase = JOptionPane.showInputDialog(this, "Search phrase")
          if (phrase.isNotBlank()) {
            val newItems = TmdbApi.searchMovie(phrase)
            if (newItems.isNotEmpty()) {
              val c = combo
              c.model = DefaultComboBoxModel(newItems.toTypedArray())
              c.revalidate()
              c.repaint()
            }
          }
        }
      }, BorderLayout.EAST)
      row.border = EmptyBorder(5, 5, 5, 5)
      p.add(row)
      p.add(JSeparator())
    }
    
    
    return JScrollPane(p)
  }
  
  data class CalcPaths(val input: Path, val target: Path, val item: SearchItem, val rootDir: Path) {
    val subtitles = Files.list(input.parent)
      .asSequence()
      .filter { it.nameWithoutExtension == input.nameWithoutExtension && SUBS.contains(it.extension) }
      .map { it to target.parent.resolve(target.nameWithoutExtension + "." + it.extension) }
      .toSet()
    
    val nfoPath = target.parent.resolve(target.nameWithoutExtension + ".nfo")
    
    override fun toString(): String =
      "--> ${item.name()} | $input\n" +
          "\t File: ${target.relativeTo(rootDir)}\n" +
          "\t Nfo: ${nfoPath.relativeTo(rootDir)}\n" +
          "\t Subs: ${subtitles.joinToString("\n\t\t", "\n\t\t") { " * ${it.first.relativeTo(rootDir)} -> ${it.second.relativeTo(rootDir)}" }}"
  }
}
