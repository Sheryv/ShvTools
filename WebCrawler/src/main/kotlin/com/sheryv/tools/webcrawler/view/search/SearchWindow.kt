package com.sheryv.tools.webcrawler.view.search

import com.formdev.flatlaf.FlatDarkLaf
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataExternalChangeEvent
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.DirectUrl
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.streamingwebsite.idm.IDMService
import com.sheryv.tools.webcrawler.service.videosearch.SearchItem
import com.sheryv.tools.webcrawler.service.videosearch.TmdbApi
import com.sheryv.tools.webcrawler.service.videosearch.TmdbEpisode
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.tools.webcrawler.utils.inBackground
import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.tools.webcrawler.utils.postEvent
import com.sheryv.util.FileUtils
import com.sheryv.util.Strings
import org.apache.commons.lang3.StringUtils
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.function.Consumer
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SearchWindow {
  var mainPanel: JPanel? = null
    private set
  private lateinit var properties: JPanel
  private lateinit var episodes: JPanel
  private lateinit var searchText: JTextField
  private lateinit var searchBtn: JButton
  private lateinit var clearBtn: JButton
  private lateinit var saveToFileBtn: JButton
  private lateinit var seasonNumber: JSpinner
  private lateinit var detailsField: JTextField
  private lateinit var nameField: JTextField
  private lateinit var showFileListButton: JButton
  private lateinit var progressBar: JProgressBar
  private lateinit var findSizeBtn: JButton
  private lateinit var cbVersion: JComboBox<SearchItem>
  private val api = TmdbApi()
  private var lastSeries: Series? = null
  private lateinit var configuration: Configuration
  private lateinit var settings: StreamingWebsiteSettings
  
  fun init(config: Configuration, settings: StreamingWebsiteSettings): SearchWindow {
    configuration = config
    this.settings = settings
    seasonNumber.value = 1
    val font: Font
    font = try {
      Font("Consolas", Font.PLAIN, cbVersion.font.size)
    } catch (e: Exception) {
      e.printStackTrace()
      Font("monospaced", Font.PLAIN, cbVersion.font.size)
    }
    cbVersion.font = font
    searchBtn.addActionListener {
      detailsField.text = ""
      nameField.text = ""
      val result = api.searchTv(searchText.text)
      if (result.isNotEmpty()) {
        cbVersion.removeAllItems()
        for (searchItem in result) {
          cbVersion.addItem(searchItem)
        }
        cbVersion.selectedIndex = 0
      }
    }
    cbVersion.addActionListener { e: ActionEvent? ->
      val item = cbVersion.selectedItem
      if (item != null) {
        onVersionChanged(item as SearchItem)
      }
    }
    clearBtn.addActionListener {
      lastSeries = lastSeries!!.copy(episodes = emptyList())
      fillList(lastSeries)
    }
    saveToFileBtn.addActionListener {
      if (lastSeries != null) {
        setProgress(-1)
        doInBackground(
          null,
          {
            Utils.jsonMapper.writeValue(settings.outputPath.toFile(), lastSeries)
            postEvent(FetchedDataExternalChangeEvent())
          },
          null,
          { setProgress(100) }
        )
      }
    }
    showFileListButton.addActionListener { showFileNamesList(lastSeries) }
    findSizeBtn.addActionListener {
      if (lastSeries == null) return@addActionListener
      setProgress(-1)
      findSizeBtn.isEnabled = false
      val part = 100f / lastSeries!!.episodes.size
      doInBackground<Series, Any?>(
        lastSeries!!,
        {
          var prog = 0f
          for (episode in lastSeries!!.episodes) {
            try {
              val size = getDownloadSize(episode.downloadUrl?.base.orEmpty())
              episode.lastSize = size
            } catch (e: Exception) {
              lg().error("Cant get video size: " + episode.generateFileName(lastSeries!!, settings), e)
            }
            prog += part
            setProgress(prog.toInt())
            EventQueue.invokeLater { fillList(lastSeries) }
          }
          
          null
        },
        null,
        {
          findSizeBtn.isEnabled = true
          setProgress(100)
          fillList(lastSeries)
        }
      )
    }
    episodes.layout = BoxLayout(episodes, BoxLayout.Y_AXIS)
    episodes.alignmentY = 0f
    loadFromFile()
    return this
  }
  
  private fun loadFromFile() {
    setProgress(-1)
    doInBackground<Any?, Series>(
      null,
      { Utils.jsonMapper.readValue(settings.outputPath.toFile(), Series::class.java) },
      {
        seasonNumber.value = it.season
        searchText.text = it.title
        fillList(it)
      },
      { setProgress(100) }
    )
  }
  
  private fun fillList(season: Series?) {
    episodes.removeAll()
    if (season == null) return
    lastSeries = season
    val seasonEpisodes = season.episodes.toMutableList()
    for (i in seasonEpisodes.indices) {
      val episode: Episode = seasonEpisodes[i]
      val toIdmBtn = JButton("IDM")
      toIdmBtn.margin = Insets(2, 2, 1, 2)
      toIdmBtn.addActionListener { IDMService(configuration).addSingle(season, episode, settings) }
      val loadSizeBtn = JButton("Size")
      loadSizeBtn.margin = Insets(2, 2, 1, 2)
      loadSizeBtn.addActionListener { loadSize(episode) }
      val sizeString = if (episode.lastSize == 0L) "" else FileUtils.sizeString(episode.lastSize)
      val row: JComponent = appendRow(
        episodes, String.format("%02d [%s] %s", episode.number, sizeString, episode.title), episode.downloadUrl?.base,
        String::class.java, {
          if (it.isNotBlank()) {
            seasonEpisodes[i] = episode.copy(downloadUrl = DirectUrl(it), errors = emptyList())
            lastSeries = lastSeries!!.copy(episodes = seasonEpisodes)
          }
        }, listOf(toIdmBtn, loadSizeBtn)
      )
//      if (i % 2 != 0) {
//        row.background = Color(223, 223, 223)
//      }
      /* row.addMouseListener(new MouseAdapter() {
                private Border grayBorder = BorderFactory.createLineBorder(Color.DARK_GRAY, 2);
                private Border prev;

                public void mouseEntered(MouseEvent e) {
                    JPanel parent = (JPanel) e.getSource();
                    prev = parent.getBorder();
                    parent.setBorder(grayBorder);
                    parent.revalidate();
                }

                public void mouseExited(MouseEvent e) {

                    Component c = SwingUtilities.getDeepestComponentAt(
                            e.getComponent(), e.getX(), e.getY());
                    // doesn't work if you move your mouse into the combobox popup
                    if (c == null) {
                        JPanel parent = (JPanel) e.getSource();
                        parent.setBorder(prev);
                        parent.revalidate();
                    }
                }
            });*/
    }
    episodes.updateUI()
  }
  
  private fun onVersionChanged(i: SearchItem) {
    setProgress(-1)
    searchBtn.isEnabled = false
    val season = seasonNumber.value as Int
    setDetails(i, season)
    fillList(null)
    doInBackground(i, {
      api.getTvEpisodes(i.id, season).episodes.map { ep: TmdbEpisode ->
        
        val found = lastSeries!!.episodes.firstOrNull { l -> l.number == ep.episodeNumber && season == lastSeries!!.season }
        
        if (found != null) {
          return@map found.copy(title = ep.name).apply { lastSize = found.lastSize }
        }
        Episode(ep.name, ep.episodeNumber, null, "")
      } to api.getImdbId(i.id)
    },
      { (episodeList, imdb) ->
        if (episodeList.isNotEmpty()) {
          val series = Series(
            i.name,
            season,
            lastSeries!!.lang,
            lastSeries!!.seriesUrl,
            i.posterUrl(),
            i.id.toString(),
            imdb,
            i.firstAirDate?.let { LocalDate.parse(it) },
            episodeList
          )
          fillList(series)
        }
      }, {
        searchBtn.isEnabled = true
        setProgress(100)
      })
  }
  
  private fun setDetails(i: SearchItem, season: Int) {
    nameField.text = StringUtils.defaultString(i.name, i.originalName)
    detailsField.text = java.lang.String.format(
      "%s [%d, %s, S%d] %s | %.2f", i.originalName,
      i.id, i.originalLanguage, season, i.firstAirDate, i.popularity
    )
  }
  
  private fun showFileNamesList(series: Series?) {
    val topPanel = JPanel()
    topPanel.layout = BoxLayout(topPanel, BoxLayout.X_AXIS)
    val textarea = JTextArea()
    textarea.isEditable = false
    val font = textarea.font
    textarea.font = font.deriveFont(font.size + 5f)
    val field = JTextField()
    field.document.addDocumentListener(object : DocumentListener {
      override fun changedUpdate(e: DocumentEvent) {}
      override fun removeUpdate(e: DocumentEvent) {}
      override fun insertUpdate(e: DocumentEvent) {
        val collect = series!!.episodes.joinToString("\n") { it.generateFileName(series, settings, field.text) }
        textarea.text = collect
      }
    })
    field.text = "mp4"
    val panel = JPanel()
    val createFilesBtn = JButton("Create empty templates files")
    createFilesBtn.addActionListener { e: ActionEvent? ->
      val fileChooser = JFileChooser(settings.downloadDir)
      fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
      val option: Int = fileChooser.showOpenDialog(panel)
      if (option == JFileChooser.APPROVE_OPTION) {
        val file: File = fileChooser.selectedFile
        val dir = Path.of(file.absolutePath).resolve(series!!.generateDirectoryPathForSeason())
        Files.createDirectories(dir)
        series.episodes.map { ep: Episode ->
          ep.generateFileName(series, settings, field.text)
        }.forEach { f: String ->
          Files.createFile(dir.resolve(f))
        }
        lg().info("Files generated in $file")
      }
    }
    topPanel.add(JLabel("Extension:"))
    topPanel.add(Box.createRigidArea(Dimension(5, 5)))
    topPanel.add(field)
    topPanel.add(Box.createRigidArea(Dimension(5, 5)))
    topPanel.add(createFilesBtn)
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.add(topPanel)
    panel.add(Box.createRigidArea(Dimension(5, 5)))
    panel.add(textarea)
    JOptionPane.showMessageDialog(null, panel, "Episodes files list", JOptionPane.PLAIN_MESSAGE)
  }
  
  private fun loadSize(episode: Episode) {
    if (episode.downloadUrl == null) return
    setProgress(-1)
    doInBackground(
      episode,
      { getDownloadSize(it.downloadUrl!!.base) },
      {
        episode.lastSize = it
        fillList(lastSeries)
      },
      { setProgress(100) }
    )
  }
  
  private fun getDownloadSize(urlString: String): Long {
    val url = URL(urlString)
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.connect()
    val contentLengthLong = urlConnection.contentLengthLong
    urlConnection.disconnect()
    return contentLengthLong
  }
  
  private fun setProgress(value: Int) {
    EventQueue.invokeLater {
      if (value < 0) {
        progressBar.isIndeterminate = true
      } else {
        if (progressBar.isIndeterminate) progressBar.isIndeterminate = false
        progressBar.value = value
      }
    }
  }
  
  private fun <T, R> doInBackground(input: T, work: (T) -> R, success: ((R) -> Unit)?, finished: ((R?) -> Unit)?) {
    inBackground {
      var result: R? = null
      try {
        result = work.invoke(input)
        if (success != null) {
          val r = result
          EventQueue.invokeLater { success.invoke(r) }
        }
      } catch (e: Exception) {
        lg().error("In Background task", e)
      }
      if (finished != null) {
        val r = result
        EventQueue.invokeLater { finished.invoke(r) }
      }
    }
  }
  
  fun <T> appendRow(panel: JPanel, label: String?, value: T, type: Class<T>, onChange: Consumer<T>): JComponent? {
    return appendRow(panel, label, value, type, onChange, emptyList<JComponent>())
  }
  
  fun <T> appendRow(
    panel: JPanel,
    label: String?,
    value: T?,
    type: Class<T>,
    onChange: Consumer<T>,
    additionalComponents: List<JComponent?>
  ): JComponent {
    val row = JPanel(BorderLayout(10, 0))
    val title = JLabel(label)
    title.minimumSize = Dimension(60, 10)
    title.preferredSize = Dimension(160, 12)
    title.toolTipText = label
    row.add(title, BorderLayout.WEST)
    row.maximumSize = Dimension(Short.MAX_VALUE.toInt(), 26)
    row.border = BorderFactory.createEmptyBorder(3, 5, 3, 5)
    val box: JComponent
    when (type) {
      String::class.java, Int::class.java, Long::class.java, Float::class.java, Double::class.java -> {
        val jTextField = JTextField(value?.toString() ?: "")
        jTextField.document.addDocumentListener(object : DocumentListener {
          override fun insertUpdate(e: DocumentEvent) {
            triggerEvent(type, jTextField, onChange)
          }
          
          override fun removeUpdate(e: DocumentEvent) {
            triggerEvent(type, jTextField, onChange)
          }
          
          override fun changedUpdate(e: DocumentEvent) {
            triggerEvent(type, jTextField, onChange)
          }
        })
        box = jTextField
      }
      Boolean::class.java -> {
        val jCheckBox = JCheckBox()
        var selected = false
        if (value != null) selected = value as Boolean
        jCheckBox.isSelected = selected
        jCheckBox.addChangeListener { e: ChangeEvent ->
          val c = e.source as JCheckBox
          onChange.accept(c.isSelected as T)
        }
        box = jCheckBox
      }
      else -> {
        throw IllegalArgumentException("Unknown type")
      }
    }
    row.add(box, BorderLayout.CENTER)
    if (box is JTextField) {
      val btns = JPanel()
      btns.layout = BoxLayout(btns, BoxLayout.X_AXIS)
      val clearBtn = JButton("X")
      clearBtn.addActionListener { e: ActionEvent? -> box.text = "" }
      val pasteBtn = JButton("P")
      pasteBtn.addActionListener { e: ActionEvent? ->
        try {
          val data = Toolkit.getDefaultToolkit()
            .systemClipboard.getData(DataFlavor.stringFlavor) as String
          lg().debug("Read from clipboard: {}", data)
          box.text = data
        } catch (ex: UnsupportedFlavorException) {
          ex.printStackTrace()
        } catch (ex: IOException) {
          ex.printStackTrace()
        }
      }
      btns.add(clearBtn)
      btns.add(pasteBtn)
      for (component in additionalComponents) {
        btns.add(component)
      }
      row.add(btns, BorderLayout.EAST)
    }
    panel.add(row)
    val separator = JPanel(true)
//    separator.background = Color(210, 210, 210)
    separator.preferredSize = Dimension(100, 1)
    separator.maximumSize = Dimension(Int.MAX_VALUE, 1)
    panel.add(separator)
    return row
  }
  
  private fun <T> triggerEvent(type: Class<T>, field: JTextField, onChange: Consumer<T>) {
    val text = field.text
    if (!Strings.isNullOrEmpty(text)) {
      try {
        val value = parseString(type, text)
        onChange.accept(value)
      } catch (e: java.lang.Exception) {
        e.printStackTrace()
      }
    }
  }
  
  
  private fun <T> parseString(type: Class<T>, value: String): T {
    if (type == Int::class.java) {
      return value.toInt() as T
    }
    if (type == Long::class.java) {
      return value.toLong() as T
    }
    if (type == Float::class.java) {
      return value.toFloat() as T
    }
    return if (type == Double::class.java) {
      value.toDouble() as T
    } else value as T
  }
  
  init {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    `$$$setupUI$$$`()
  }
  
  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private fun `$$$setupUI$$$`() {
    mainPanel = JPanel()
    mainPanel!!.layout = GridBagLayout()
    val spacer1 = JPanel()
    var gbc: GridBagConstraints
    gbc = GridBagConstraints()
    gbc.gridx = 5
    gbc.gridy = 1
    gbc.fill = GridBagConstraints.HORIZONTAL
    mainPanel!!.add(spacer1, gbc)
    val spacer2 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 2
    gbc.gridwidth = 4
    gbc.fill = GridBagConstraints.VERTICAL
    mainPanel!!.add(spacer2, gbc)
    val spacer3 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.fill = GridBagConstraints.HORIZONTAL
    mainPanel!!.add(spacer3, gbc)
    val spacer4 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 4
    gbc.fill = GridBagConstraints.VERTICAL
    mainPanel!!.add(spacer4, gbc)
    val spacer5 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 6
    gbc.gridwidth = 4
    gbc.fill = GridBagConstraints.VERTICAL
    mainPanel!!.add(spacer5, gbc)
    val panel1 = JPanel()
    panel1.layout = GridBagLayout()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 7
    gbc.gridwidth = 2
    gbc.fill = GridBagConstraints.BOTH
    mainPanel!!.add(panel1, gbc)
    val spacer6 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 8
    gbc.gridwidth = 4
    gbc.fill = GridBagConstraints.VERTICAL
    mainPanel!!.add(spacer6, gbc)
    properties = JPanel()
    properties.layout = GridBagLayout()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 1
    gbc.gridwidth = 4
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.BOTH
    mainPanel!!.add(properties, gbc)
    val label1 = JLabel()
    label1.text = "Search name"
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.anchor = GridBagConstraints.WEST
    properties.add(label1, gbc)
    val spacer7 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.fill = GridBagConstraints.VERTICAL
    properties.add(spacer7, gbc)
    searchText = JTextField()
    gbc = GridBagConstraints()
    gbc.gridx = 3
    gbc.gridy = 0
    gbc.gridwidth = 7
    gbc.weightx = 1.0
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(searchText, gbc)
    clearBtn = JButton()
    clearBtn.text = "Clear list"
    gbc = GridBagConstraints()
    gbc.gridx = 7
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(clearBtn, gbc)
    searchBtn = JButton()
    searchBtn.text = "Search"
    gbc = GridBagConstraints()
    gbc.gridx = 11
    gbc.gridy = 0
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(searchBtn, gbc)
    saveToFileBtn = JButton()
    saveToFileBtn.text = "Save to File"
    gbc = GridBagConstraints()
    gbc.gridx = 9
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(saveToFileBtn, gbc)
    val label2 = JLabel()
    label2.text = "Season"
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 2
    gbc.anchor = GridBagConstraints.WEST
    properties.add(label2, gbc)
    val spacer8 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(spacer8, gbc)
    val spacer9 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 10
    gbc.gridy = 0
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(spacer9, gbc)
    showFileListButton = JButton()
    showFileListButton.text = "Show File List"
    gbc = GridBagConstraints()
    gbc.gridx = 11
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(showFileListButton, gbc)
    val spacer10 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 8
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(spacer10, gbc)
    findSizeBtn = JButton()
    findSizeBtn.text = "Find file size"
    gbc = GridBagConstraints()
    gbc.gridx = 5
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(findSizeBtn, gbc)
    val spacer11 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 6
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(spacer11, gbc)
    seasonNumber = JSpinner()
    seasonNumber.minimumSize = Dimension(50, 30)
    seasonNumber.preferredSize = Dimension(50, 30)
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 2
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(seasonNumber, gbc)
    val spacer12 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 4
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(spacer12, gbc)
    cbVersion = JComboBox<SearchItem>()
    gbc = GridBagConstraints()
    gbc.gridx = 3
    gbc.gridy = 2
    gbc.weightx = 0.9
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(cbVersion, gbc)
    val spacer13 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 2
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    properties.add(spacer13, gbc)
    val scrollPane1 = JScrollPane()
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 5
    gbc.gridwidth = 4
    gbc.weightx = 1.0
    gbc.weighty = 0.7
    gbc.fill = GridBagConstraints.BOTH
    mainPanel!!.add(scrollPane1, gbc)
    episodes = JPanel()
    episodes.layout = FlowLayout(FlowLayout.CENTER, 5, 5)
    scrollPane1.setViewportView(episodes)
    val spacer14 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 2
    gbc.gridy = 4
    gbc.gridwidth = 3
    gbc.fill = GridBagConstraints.VERTICAL
    mainPanel!!.add(spacer14, gbc)
    detailsField = JTextField()
    detailsField.isEditable = false
    gbc = GridBagConstraints()
    gbc.gridx = 4
    gbc.gridy = 3
    gbc.weightx = 0.8
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL
    mainPanel!!.add(detailsField, gbc)
    nameField = JTextField()
    nameField.isEditable = false
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 3
    gbc.gridwidth = 2
    gbc.weightx = 0.2
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL
    mainPanel!!.add(nameField, gbc)
    val spacer15 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 3
    gbc.gridy = 3
    gbc.fill = GridBagConstraints.HORIZONTAL
    mainPanel!!.add(spacer15, gbc)
    progressBar = JProgressBar()
    progressBar.minimumSize = Dimension(10, 15)
    progressBar.preferredSize = Dimension(146, 15)
    gbc = GridBagConstraints()
    gbc.gridx = 4
    gbc.gridy = 7
    gbc.fill = GridBagConstraints.HORIZONTAL
    mainPanel!!.add(progressBar, gbc)
  }
  
  /**
   * @noinspection ALL
   */
  fun `$$$getRootComponent$$$`(): JComponent? {
    return mainPanel
  }
  
  private fun createUIComponents() {
    // TODO: place custom component creation code here
  }
}
