package com.sheryv.util.unit

enum class BitUnit(override val label: String, val shortLabel: String, val scale: Int) : MetricUnit {
  b("Bits", "b", 0),
  kb("Kilobits", "kb", 1),
  Mb("Megabits", "Mb", 2),
  Gb("Gigabits", "Gb", 3),
  Tb("Terabits", "Tb", 4),
  Pb("Petabits", "Pb", 5);
  
  override fun toString(): String = shortLabel
}
