package com.sheryv.util.fx.lib

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.StringConverter
import java.time.LocalDate

var Node.hgrow: Priority?
  get() = HBox.getHgrow(this)
  set(value) {
    HBox.setHgrow(this, value)
  }

var Node.vgrow: Priority?
  get() = VBox.getVgrow(this)
  set(value) {
    VBox.setVgrow(this, value)
  }

fun <T : Node> T.grow(prio: Priority = Priority.ALWAYS): T {
  HBox.setHgrow(this, prio)
  VBox.setVgrow(this, prio)
  return this
}

fun <T : Node> T.growH(prio: Priority = Priority.ALWAYS): T {
  HBox.setHgrow(this, prio)
  return this
}

fun <T : Node> T.growV(prio: Priority = Priority.ALWAYS): T {
  VBox.setVgrow(this, prio)
  return this
}

fun Parent.getToggleGroup(): ToggleGroup? = properties["shvtools.togglegroup"] as ToggleGroup?

enum class ColorPickerMode { Button, MenuButton, SplitMenuButton }

fun Parent.colorpicker(
  color: Color? = null,
  mode: ColorPickerMode = ColorPickerMode.Button,
  op: ColorPicker.() -> Unit = {}
) = ColorPicker().attachTo(this, op) {
  if (mode == ColorPickerMode.MenuButton) it.styleClass.add(ColorPicker.STYLE_CLASS_BUTTON)
  else if (mode == ColorPickerMode.SplitMenuButton) it.styleClass.add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)
  if (color != null) it.value = color
}

fun Parent.colorpicker(
  colorProperty: ObjectProperty<Color>,
  mode: ColorPickerMode = ColorPickerMode.Button,
  op: ColorPicker.() -> Unit = {}
) = ColorPicker().apply { bind(colorProperty) }.attachTo(this, op) {
  if (mode == ColorPickerMode.MenuButton) it.styleClass.add(ColorPicker.STYLE_CLASS_BUTTON)
  else if (mode == ColorPickerMode.SplitMenuButton) it.styleClass.add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)
}

fun Parent.textflow(op: TextFlow.() -> Unit = {}) = TextFlow().attachTo(this, op)

fun Parent.text(op: Text.() -> Unit = {}) = Text().attachTo(this, op)

internal val EventTarget.properties: ObservableMap<Any, Any>
  get() = when (this) {
    is Node -> properties
    is Tab -> properties
    is MenuItem -> properties
    else -> throw IllegalArgumentException("Don't know how to extract properties object from $this")
  }

//var Node.tagProperty: Property<Any?>
//  get() = properties.getOrPut("shvtools.value") {
//    SimpleObjectProperty<Any?>()
//  } as SimpleObjectProperty<Any?>
//  set(value) {
//    properties["shvtools.value"] = value
//  }
//
//var Node.tag: Any?
//  get() = tagProperty.value
//  set(value) {
//    tagProperty.value = value
//  }

@Deprecated("Properties set on the fake node would be lost. Do not use this function.", ReplaceWith("Manually adding children"))
fun children(addTo: MutableList<Node>, op: Pane.() -> Unit) {
  val fake = Pane().also(op)
  addTo.addAll(fake.children)
}


fun Parent.text(initialValue: String? = null, op: Text.() -> Unit = {}) = Text().attachTo(this, op) {
  if (initialValue != null) it.text = initialValue
}

fun Parent.text(property: Property<String>, op: Text.() -> Unit = {}) = text().apply {
  bind(property)
  op(this)
}

fun Parent.text(observable: ObservableValue<String>, op: Text.() -> Unit = {}) = text().apply {
  bind(observable)
  op(this)
}

fun Parent.textfield(value: String? = null, op: TextField.() -> Unit = {}) = TextField().attachTo(this, op) {
  if (value != null) it.text = value
}

fun Parent.textfield(property: ObservableValue<String>, op: TextField.() -> Unit = {}) = textfield().apply {
  bind(property)
  op(this)
}

@JvmName("textfieldNumber")
fun Parent.textfield(property: ObservableValue<Number>, op: TextField.() -> Unit = {}) = textfield().apply {
  bind(property)
  op(this)
}

@JvmName("textfieldInt")
fun Parent.textfield(property: ObservableValue<Int>, op: TextField.() -> Unit = {}) = textfield().apply {
  bind(property)
  op(this)
}

@JvmName("textfieldDouble")
fun Parent.textfield(property: ObservableValue<Double>, op: TextField.() -> Unit = {}) = textfield().apply {
  bind(property)
  op(this)
}

fun Parent.passwordfield(value: String? = null, op: PasswordField.() -> Unit = {}) = PasswordField().attachTo(this, op) {
  if (value != null) it.text = value
}

fun Parent.passwordfield(property: ObservableValue<String>, op: PasswordField.() -> Unit = {}) = passwordfield().apply {
  bind(property)
  op(this)
}

fun <T> Parent.textfield(property: Property<T>, converter: StringConverter<T>, op: TextField.() -> Unit = {}) = textfield().apply {
  textProperty().bindBidirectional(property, converter)
  op(this)
}

fun Parent.datepicker(op: DatePicker.() -> Unit = {}) = DatePicker().attachTo(this, op)
fun Parent.datepicker(property: Property<LocalDate>, op: DatePicker.() -> Unit = {}) = datepicker().apply {
  bind(property)
  op(this)
}

fun Parent.textarea(value: String? = null, op: TextArea.() -> Unit = {}) = TextArea().attachTo(this, op) {
  if (value != null) it.text = value
}

fun Parent.textarea(property: ObservableValue<String>, op: TextArea.() -> Unit = {}) = textarea().apply {
  bind(property)
  op(this)
}

fun <T> Parent.textarea(property: Property<T>, converter: StringConverter<T>, op: TextArea.() -> Unit = {}) = textarea().apply {
  textProperty().bindBidirectional(property, converter)
  op(this)
}

fun Parent.buttonbar(buttonOrder: String? = null, op: (ButtonBar.() -> Unit)) = ButtonBar().attachTo(this, op) {
  if (buttonOrder != null) it.buttonOrder = buttonOrder
}

fun Parent.checkbox(text: String? = null, property: Property<Boolean>? = null, op: CheckBox.() -> Unit = {}) =
  CheckBox(text).attachTo(this, op) {
    if (property != null) it.bind(property)
  }

fun <T> Parent.combo(entries: ObservableList<T>, property: Property<T>? = null, op: ComboBox<T>.() -> Unit = {}) =
  ComboBox(entries).attachTo(this, op) {
    if (property != null) it.bind(property)
  }


fun Parent.progressindicator(op: ProgressIndicator.() -> Unit = {}) = ProgressIndicator().attachTo(this, op)
fun Parent.progressindicator(property: Property<Number>, op: ProgressIndicator.() -> Unit = {}) = progressindicator().apply {
  bind(property)
  op(this)
}

fun Parent.progressbar(initialValue: Double? = null, op: ProgressBar.() -> Unit = {}) = ProgressBar().attachTo(this, op) {
  if (initialValue != null) it.progress = initialValue
}

fun Parent.progressbar(property: ObservableValue<Number>, op: ProgressBar.() -> Unit = {}) = progressbar().apply {
  bind(property)
  op(this)
}

fun Parent.slider(
  min: Number? = null,
  max: Number? = null,
  value: Number? = null,
  orientation: Orientation? = null,
  op: Slider.() -> Unit = {}
) = Slider().attachTo(this, op) {
  if (min != null) it.min = min.toDouble()
  if (max != null) it.max = max.toDouble()
  if (value != null) it.value = value.toDouble()
  if (orientation != null) it.orientation = orientation
}

fun <T> Parent.slider(
  range: ClosedRange<T>,
  value: Number? = null,
  orientation: Orientation? = null,
  op: Slider.() -> Unit = {}
): Slider
    where T : Comparable<T>,
          T : Number {
  return slider(range.start, range.endInclusive, value, orientation, op)
}


// Buttons
fun Parent.button(text: String = "", graphic: Node? = null, op: Button.() -> Unit = {}) = Button(text).attachTo(this, op) {
  if (graphic != null) it.graphic = graphic
}

fun Parent.menubutton(text: String = "", graphic: Node? = null, op: MenuButton.() -> Unit = {}) = MenuButton(text).attachTo(this, op) {
  if (graphic != null) it.graphic = graphic
}

fun Parent.splitmenubutton(text: String? = null, graphic: Node? = null, op: SplitMenuButton.() -> Unit = {}) =
  SplitMenuButton().attachTo(this, op) {
    if (text != null) it.text = text
    if (graphic != null) it.graphic = graphic
  }

fun Parent.button(text: ObservableValue<String>, graphic: Node? = null, op: Button.() -> Unit = {}) = Button().attachTo(this, op) {
  it.textProperty().bind(text)
  if (graphic != null) it.graphic = graphic
}

fun ToolBar.button(text: String = "", graphic: Node? = null, op: Button.() -> Unit = {}) = Button(text).also {
  if (graphic != null) it.graphic = graphic
  items += it
  op(it)
}

fun ToolBar.button(text: ObservableValue<String>, graphic: Node? = null, op: Button.() -> Unit = {}) = Button().also {
  it.textProperty().bind(text)
  if (graphic != null) it.graphic = graphic
  items += it
  op(it)
}

/**
 * @param icon Icon code from https://fonts.google.com/icons?selected=Material+Symbols+Outlined:add_2:FILL@0;wght@400;GRAD@0;opsz@24&icon.size=24&icon.color=%23e3e3e3&icon.query=add&icon.set=Material+Symbols
 */
fun Parent.buttonIcon(icon: String = "", text: String = "", tooltip: String? = null, large: Boolean = false, op: Button.() -> Unit = {}) =
  Button(text).attachTo(this, op) {
    val iconLabel = Label(icon)
    if (large) {
      styleClass += "if-btn-large-wrapper"
      iconLabel.styleClass += "if-btn-label-large"
    } else {
      iconLabel.styleClass += "if-btn-label"
    }
    it.graphic = iconLabel
    if (tooltip != null) it.tooltip = Tooltip(tooltip)
  }

/**
 * @param icon Icon code from https://fonts.google.com/icons?selected=Material+Symbols+Outlined:add_2:FILL@0;wght@400;GRAD@0;opsz@24&icon.size=24&icon.color=%23e3e3e3&icon.query=add&icon.set=Material+Symbols
 */
fun ToolBar.buttonIcon(icon: String = "", text: String = "", tooltip: String? = null, large: Boolean = false, op: Button.() -> Unit = {}) =
  Button(text).also {
    val iconLabel = Label(icon)
    if (large) {
      styleClass += "if-btn-large-wrapper"
      iconLabel.styleClass += "if-btn-label-large"
    } else {
      iconLabel.styleClass += "if-btn-label"
    }
    it.graphic = iconLabel
    if (tooltip != null) it.tooltip = Tooltip(tooltip)
    items += it
    op(it)
  }

fun ToolBar.buttonIcon(
  icon: ObservableValue<String>,
  text: ObservableValue<String>? = null,
  tooltip: String? = null,
  large: Boolean = false,
  op: Button.() -> Unit = {}
) = Button().also {
  text?.also { t -> it.textProperty().bind(t) }
  val iconLabel = Label()
  iconLabel.textProperty().bind(icon)
  if (large) {
    styleClass += "if-btn-large-wrapper"
    iconLabel.styleClass += "if-btn-label-large"
  } else {
    iconLabel.styleClass += "if-btn-label"
  }
  it.graphic = iconLabel
  if (tooltip != null) it.tooltip = Tooltip(tooltip)
  items += it
  op(it)
}

fun ButtonBar.button(text: String = "", type: ButtonBar.ButtonData? = null, graphic: Node? = null, op: Button.() -> Unit = {}) =
  Button(text).also {
    if (type != null) ButtonBar.setButtonData(it, type)
    if (graphic != null) it.graphic = graphic
    buttons += it
    op(it)
  }

fun ButtonBar.button(text: ObservableValue<String>, type: ButtonBar.ButtonData? = null, graphic: Node? = null, op: Button.() -> Unit = {}) =
  Button().also {
    it.textProperty().bind(text)
    if (type != null) ButtonBar.setButtonData(it, type)
    if (graphic != null) it.graphic = graphic
    buttons += it
    op(it)
  }

fun Parent.togglegroup(property: ObservableValue<Any>? = null, op: ToggleGroup.() -> Unit = {}) = ToggleGroup().also { tg ->
  properties["shvtools.togglegroup"] = tg
  property?.let { tg.bind(it) }
  op(tg)
}


/**
 * Bind the selectedValueProperty of this toggle group to the given property. Passing in a writeable value
 * will result in a bidirectional binding, while passing in a read only value will result in a unidirectional binding.
 *
 * If the toggles are configured with the value parameter (@see #togglebutton and #radiogroup), the corresponding
 * button will be selected when the value is changed. Likewise, if the selected toggle is changed,
 * the property value will be updated if it is writeable.
 */
fun <T> ToggleGroup.bind(property: ObservableValue<T>) = selectedValueProperty<T>().apply {
  (property as? Property<T>)?.also { bindBidirectional(it) }
    ?: bind(property)
}

/**
 * Generates a writable property that represents the selected value for this toggele group.
 * If the toggles are configured with a value (@see #togglebutton and #radiogroup) the corresponding
 * toggle will be selected when this value is changed. Likewise, if the toggle is changed by clicking
 * it, the value for the toggle will be written to this property.
 *
 * To bind to this property, use the #ToggleGroup.bind() function.
 */
fun <T> ToggleGroup.selectedValueProperty(): ObjectProperty<T> = properties.getOrPut("shvtools.selectedValueProperty") {
  SimpleObjectProperty<T>().apply {
    selectedToggleProperty().onChange {
      value = it?.properties?.get("shvtools.toggleGroupValue") as T?
    }
    onChange { selectedValue ->
      selectToggle(toggles.find { it.properties["shvtools.toggleGroupValue"] == selectedValue })
    }
  }
} as ObjectProperty<T>

/**
 * Create a togglebutton inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the #ToggleGroup.selectedValueProperty is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * togglebutton, it will be automatically selected.
 */
fun Parent.togglebutton(
  text: String? = null,
  group: ToggleGroup? = getToggleGroup(),
  selectFirst: Boolean = true,
  value: Any? = null,
  op: ToggleButton.() -> Unit = {}
) = ToggleButton().attachTo(this, op) {
  it.text = if (value != null && text == null) value.toString() else text ?: ""
  it.properties["shvtools.toggleGroupValue"] = value ?: text
  if (group != null) it.toggleGroup = group
  if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}

fun Parent.togglebutton(
  text: ObservableValue<String>? = null,
  group: ToggleGroup? = getToggleGroup(),
  selectFirst: Boolean = true,
  value: Any? = null,
  op: ToggleButton.() -> Unit = {}
) = ToggleButton().attachTo(this, op) {
  it.textProperty().bind(text)
  it.properties["shvtools.toggleGroupValue"] = value ?: text
  if (group != null) it.toggleGroup = group
  if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}

fun Parent.togglebutton(
  group: ToggleGroup? = getToggleGroup(),
  selectFirst: Boolean = true,
  value: Any? = null,
  op: ToggleButton.() -> Unit = {}
) = ToggleButton().attachTo(this, op) {
  it.properties["shvtools.toggleGroupValue"] = value
  if (group != null) it.toggleGroup = group
  if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}

fun ToggleButton.whenSelected(op: () -> Unit) {
  selectedProperty().onChange { if (it) op() }
}

/**
 * Create a radiobutton inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the #ToggleGroup.selectedValueProperty is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * radiobutton, it will be automatically selected.
 */
fun Parent.radiobutton(
  text: String? = null,
  group: ToggleGroup? = getToggleGroup(),
  value: Any? = null,
  op: RadioButton.() -> Unit = {}
) = RadioButton().attachTo(this, op) {
  it.text = if (value != null && text == null) value.toString() else text ?: ""
  it.properties["shvtools.toggleGroupValue"] = value ?: text
  if (group != null) it.toggleGroup = group
}

fun Parent.label(text: String = "", graphic: Node? = null, op: Label.() -> Unit = {}) = Label(text).attachTo(this, op) {
  if (graphic != null) it.graphic = graphic
}

fun Parent.labelWrap(text: String = "", child: Node, op: Label.() -> Unit = {}) = vbox {
  spacing = 2.0
  Label(text).attachTo(this, op)
  child.attachTo(this)
}

fun Parent.labelWrap(text: String = "", child: Node, graphic: Node, op: Label.() -> Unit = {}) = vbox {
  spacing = 2.0
  Label(text).attachTo(this, op) {
    it.graphic = graphic
  }
  child.attachTo(this)
}

inline fun <reified T> Parent.label(
  observable: ObservableValue<T>,
  graphicProperty: ObservableValue<Node>? = null,
  converter: StringConverter<in T>? = null,
  noinline op: Label.() -> Unit = {}
) = label().apply {
  if (converter == null) {
    if (T::class == String::class) {
      @Suppress("UNCHECKED_CAST")
      textProperty().bind(observable as ObservableValue<String>)
    } else {
      textProperty().bind(observable.stringBinding { it?.toString() })
    }
  } else {
    textProperty().bind(observable.stringBinding { converter.toString(it) })
  }
  if (graphic != null) graphicProperty().bind(graphicProperty)
  op(this)
}

fun Parent.hyperlink(text: String = "", graphic: Node? = null, op: Hyperlink.() -> Unit = {}) = Hyperlink(text, graphic).attachTo(this, op)
fun Parent.hyperlink(observable: ObservableValue<String>, graphic: Node? = null, op: Hyperlink.() -> Unit = {}) =
  hyperlink(graphic = graphic).apply {
    bind(observable)
    op(this)
  }

fun Parent.menubar(op: MenuBar.() -> Unit = {}) = MenuBar().attachTo(this, op)

fun Parent.imageview(url: String? = null, lazyload: Boolean = true, op: ImageView.() -> Unit = {}) =
  (if (url == null) ImageView() else ImageView(Image(url, lazyload))).attachTo(this, op)

fun Parent.imageview(
  url: ObservableValue<String>,
  lazyload: Boolean = true,
  op: ImageView.() -> Unit = {}
) = ImageView().attachTo(this, op) { imageView ->
  imageView.imageProperty().bind(objectBinding(url) { value?.let { Image(it, lazyload) } })
}

fun Parent.imageview(image: ObservableValue<Image?>, op: ImageView.() -> Unit = {}) = ImageView().attachTo(this, op) {
  it.imageProperty().bind(image)
}

fun Parent.imageview(image: Image, op: ImageView.() -> Unit = {}) = ImageView(image).attachTo(this, op)

/**
 * Listen to changes and update the value of the property if the given mutator results in a different value
 */
fun <T : Any?> Property<T>.mutateOnChange(mutator: (T?) -> T?) = onChange {
  val changed = mutator(value)
  if (changed != value) value = changed
}

/**
 * Remove leading or trailing whitespace from a Text Input Control.
 */
fun TextInputControl.trimWhitespace() = focusedProperty().onChange { focused ->
  if (!focused && text != null) text = text.trim()
}

/**
 * Remove any whitespace from a Text Input Control.
 */
fun TextInputControl.stripWhitespace() = textProperty().mutateOnChange { it?.replace(Regex("\\s*"), "") }

/**
 * Remove any non integer values from a Text Input Control.
 */
fun TextInputControl.stripNonInteger() = textProperty().mutateOnChange { it?.replace(Regex("[^0-9-]"), "") }

/**
 * Remove any non integer values from a Text Input Control.
 */
fun TextInputControl.stripNonNumeric(vararg allowedChars: String = arrayOf(".", ",", "-")): TextInputControl {
  textProperty().mutateOnChange { it?.replace(Regex("[^0-9${allowedChars.joinToString("")}]"), "") }
  return this
}

inline fun <reified T : Any> TextInputControl.withFormatter(default: T): TextFormatter<T> {
  val formatter = textFormatter<T>(default)
  textFormatter = formatter
  return formatter
}

fun ChoiceBox<*>.action(op: () -> Unit) = setOnAction { op() }
fun ButtonBase.action(op: () -> Unit): ButtonBase {
  setOnAction { op() }
  return this
}

fun TextField.action(op: () -> Unit) = setOnAction { op() }
fun MenuItem.action(op: () -> Unit) = setOnAction { op() }

fun TextArea.fixBlurring(): TextArea {
  val sp = childrenUnmodifiable[0] as ScrollPane
  sp.isCache = false
  for (n in sp.childrenUnmodifiable) {
    n.isCache = false
  }
  return this
}

fun ScrollPane.fixBlurring(): ScrollPane {
  isCache = false
  for (n in childrenUnmodifiable) {
    n.isCache = false
  }
  return this
}

fun <T : Control> T.strech(): T {
  maxWidth = Double.MAX_VALUE
  return this
}

// -- TreeItem helpers
/**
 * Expand this [TreeItem] and children down to `depth`.
 */
fun <T> TreeItem<T>.expandTo(depth: Int) {
  if (depth > 0) {
    this.isExpanded = true
    this.children.forEach { it.expandTo(depth - 1) }
  }
}

/**
 * Expand this `[TreeItem] and all it's children.
 */
fun <T> TreeItem<T>.expandAll() = expandTo(Int.MAX_VALUE)

/**
 * Collapse this [TreeItem] and all it's children.
 */
fun <T> TreeItem<T>.collapseAll() {
  this.isExpanded = false
  this.children.forEach { it.collapseAll() }
}


fun <S, T> TableView<S>.column(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  value: (S) -> T
): TableColumn<S, T> {
  return columnBound(name, width, alignRight) { ReadOnlyObjectWrapper(value(it)) }
}

fun <S, T> TableView<S>.columnBound(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  value: (S) -> ObservableValue<T>
): TableColumn<S, T> {
  val col = TableColumn<S, T>(name).apply {
    if (alignRight) {
      this.style = "-fx-alignment: CENTER-RIGHT;"
    }
    if (width != null) {
      prefWidth = width.toDouble()
    }
    setCellValueFactory { o ->
      val p1 = o.value
      value(p1)
    }
    
  }
  this.columns.add(col)
  return col
}

fun <S, T> TableView<S>.columnBoundCustom(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  value: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>
): TableColumn<S, T> {
  val col = TableColumn<S, T>(name).apply {
    if (alignRight) {
      this.style = "-fx-alignment: CENTER-RIGHT;"
    }
    if (width != null) {
      prefWidth = width.toDouble()
    }
    setCellValueFactory { o ->
      value(o)
    }
  }
  this.columns.add(col)
  return col
}

fun <S, T : Any, N : Node> TableView<S>.columnBoundToCustomCell(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  modelValue: (S) -> Property<T?>,
  builder: (TableCell<S, T?>) -> N,
  controlValue: (N) -> Property<T?>,
  onUpdate: (T?, S?, N?) -> Unit = { _, _, _ -> }
): TableColumn<S, T?> {
  return columnBound(name, width, alignRight, modelValue).customCell2(modelValue, builder, controlValue, onUpdate)
}


fun <S, T : Any, N : Node> TableColumn<S, T?>.customCell2(
  modelValue: (S) -> Property<T?>,
  builder: (TableCell<S, T?>) -> N,
  controlValue: (N) -> Property<T?>,
  onUpdate: (T?, S?, N?) -> Unit = { _, _, _ -> }
): TableColumn<S, T?> {
  setCellFactory { col ->
    val c = object : TableCell<S, T?>() {
      val node = builder(this)
      val controlProperty = controlValue(node)
      var lastListener: ChangeListener<T?>? = null
      var lastRow: S? = null
      
      override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        lastListener?.also { controlProperty.removeListener(it) }
        if (empty || tableRow == null || tableRow.item == null) {
          onUpdate(null, tableRow.item, graphic as? N)
          lastListener = null
          graphic = null
        } else {
          if (lastRow != tableRow.item) {
            lastRow = tableRow.item
            val model = modelValue(tableRow.item)
            if (controlProperty.value != model.value) {
              controlProperty.value = model.value
            }
            lastListener = ChangeListener { _, o, n ->
//                log.debug("ChangeListener {} | {}", n, model.value)
              model.value = n
            }

//              log.debug("Binding {} {} -> {} | {} | {}", tableRow.item, model.hashCode(), controlProperty.hashCode(), item, this.hashCode())
          }
          if (lastListener != null) {
            controlProperty.addListener(lastListener)
          }
          onUpdate(item, tableRow.item, node)
          graphic = node
        }
      }
    }
    c
  }
  return this
}
/*
*              TextField().stripNonNumeric().let {
                  val formatter = cell.itemProperty().formatter()
                  it.textFormatter = formatter
                  it to formatter.valueProperty()
                }
* */

fun <S, N : TextInputControl> TableView<S>.columnBoundToTextControlCell(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  modelValue: (S) -> Property<String?>,
  builder: (TableCell<S, String?>) -> N,
  controlValue: (N) -> Property<String?> = { it.textProperty() },
  onUpdate: (String?, S?, N?) -> Unit = { _, _, _ -> }
): TableColumn<S, String?> {
  
  return columnBoundToCustomCell(name, width, alignRight, modelValue, {
    it.styleClass.add("text-field-table-cell")
    builder(it)
  }, controlValue, onUpdate)
}

fun <S> TableView<S>.columnBoundToTextFieldCell(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  modelValue: (S) -> Property<String?>,
  builder: (TableCell<S, String?>) -> TextField = { TextField() },
  controlValue: (TextField) -> Property<String?> = { it.textProperty() },
  onUpdate: (String?, S?, TextField?) -> Unit = { _, _, _ -> }
): TableColumn<S, String?> {
  
  return columnBoundToCustomCell(name, width, alignRight, modelValue, {
    it.styleClass.add("text-field-table-cell")
    builder(it)
  }, controlValue, onUpdate)
}

inline fun <S, reified T : Any, N : TextInputControl> TableView<S>.columnBoundToTextFieldFormattedCell(
  name: String,
  default: T,
  width: Int? = null,
  alignRight: Boolean = false,
  noinline modelValue: (S) -> Property<T?>,
  noinline builder: (TableCell<S, T?>) -> N = { TextField() as N },
  noinline controlValue: (N) -> Property<T?> = { it.withFormatter<T>(default).valueProperty() },
  noinline onUpdate: (T?, S?, N?) -> Unit = { _, _, _ -> }
): TableColumn<S, T?> {
  
  return columnBoundToCustomCell(name, width, alignRight, modelValue, {
    it.styleClass.add("text-field-table-cell")
    builder(it)
  }, controlValue, onUpdate)

//  return customCell({ c ->
//    c.styleClass.add("text-field-table-cell")
//    builder(c).let { it to it.withFormatter<T>(default).valueProperty() }
//  }, { item, row, tf ->
//    @Suppress("UNCHECKED_CAST")
//    (tf.textFormatter as TextFormatter<T>).value = item
//    onUpdate(item, row, tf)
//  })
}

fun <S, T : Any, N : ComboBoxBase<T>> TableView<S>.columnBoundToComboCell(
  name: String,
  width: Int? = null,
  alignRight: Boolean = false,
  modelValue: (S) -> Property<T?>,
  default: ObservableList<T>? = null,
  builder: (TableCell<S, T?>) -> N = { ComboBox<T>(default ?: FXCollections.emptyObservableList()) as N },
  controlValue: (N) -> Property<T?> = { it.valueProperty() },
  onUpdate: (T?, S?, N?) -> Unit = { _, _, _ -> }
): TableColumn<S, T?> {
  return columnBoundToCustomCell(name, width, alignRight, modelValue, {
    styleClass.add("combo-box-table-cell")
    builder(it)
  }, controlValue, onUpdate)
}

fun <S, T : Any, N : ButtonBase> TableColumn<S, T>.customButtonCell(
  builder: (TableCell<S, T>) -> N,
  onUpdate: (T?, S?, N) -> Unit = { _, _, _ -> }
): TableColumn<S, T> {
  return customCell({ c -> builder(c) to null }, onUpdate)
}


fun <S, T : Any, N : Node> TableColumn<S, T>.customCell(
  builder: (TableCell<S, T>) -> Pair<N, ObservableValue<T?>?>,
  onUpdate: (T?, S?, N) -> Unit
): TableColumn<S, T> {
  isEditable = true
  setCellFactory { col ->
    val c = object : TableCell<S, T>() {
      private var columnProperty: ObservableValue<T>? = null
      fun getProp(): ObservableValue<T>? {
        if (columnProperty == null) {
          columnProperty = tableRow?.item?.let { cellValueFactory.call(TableColumn.CellDataFeatures(col.tableView, col, tableRow.item)) }
        }
        return columnProperty
      }
      
      val node = builder(this).let {
        it.second?.onChangeNotNull { value ->
          val prop = getProp()
          if (prop is Property<T>) {
            prop.value = value
          }
//          startEdit()
//          commitEdit(value)
        
        }
        it.first
      }
      
      override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || tableRow == null || tableRow.item == null) {
          graphic = null
          onUpdate(null, tableRow.item, node)
        } else {
          onUpdate(item, tableRow.item, node)
          
          graphic = node
        }
      }
    }
//    val (node, prop) = builder(c)
//
//    c.itemProperty().addListener { observable, oldValue, newValue ->
//      if (oldValue != null) {
//        prop.unbindBidirectional(c.itemProperty())
//      }
//      if (newValue != null) {
//        prop.bindBidirectional(c.itemProperty())
//      }
//    }
//    c.graphicProperty().bind(Bindings.`when`(c.emptyProperty()).then(null as Node?).otherwise(node))
    c
  }
  return this
}
