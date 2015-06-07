package averroes.experiments.util

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION

/**
 * Some math utilities.
 */
object Math {

  def round(value: Double, places: Int = 2): Double = {
    assert(places >= 0, "Can't round to -ve decimal places.")

    val factor = math.pow(10, places);
    val tmp = math.round(value * factor);
    tmp / factor;
  }

  def round(value: String): Double = round(value.toDouble)

  def percentage(value: Double, denom: Double) = {
    round(value * 100 / denom).toInt
  }
  
  // Convert memory from Kb to Mb
  def kb2mb(size: Long) = round(size / 1024d / 1024d)
}