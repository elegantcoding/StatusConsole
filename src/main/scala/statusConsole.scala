package com.elegantcoding.statuscosole

import com.googlecode.lanterna.TerminalFacade
import java.nio.charset.Charset

import scala.collection.mutable.StringBuilder

case class MovingAverage(name: String, interval: Long)

case class DisplayUnit(name: String, unit: Long)

object DisplayUnitMillion extends DisplayUnit("Million", 1000000l)

object DisplayUnitThousand extends DisplayUnit("Thousand", 1000l)

object DisplayUnitDefault extends DisplayUnit("", 1l)

class ItemCountStatus(val name: String,
                      private val movingAveragesParam: Seq[MovingAverage] = Nil,
                      val displayUnit: DisplayUnit = DisplayUnitDefault,
                      val showUnit: Boolean = true,
                      val showAverage: Boolean = false) {

  val startTime = System.currentTimeMillis
  var count: Long = 0
  var lastAvgTime = System.currentTimeMillis

  def incCount = count = count + 1

  def countInUnit = count / displayUnit.unit

  // There must be a better way to do this: have this be both publicly accessible and an inner class

  val movingAverages = movingAveragesParam.map {
    new MovingAverageImpl(_)
  }

  class MovingAverageImpl(private val movingAverage: MovingAverage) {

    val name = movingAverage.name
    val interval = movingAverage.interval

    private var movingAverageValues = Seq[(Long, Long)]((0, 0))

    def latestMovingAvg: Double = {

      val elapsed = System.currentTimeMillis - startTime

      movingAverageValues ++= Seq((count, elapsed))

      while (movingAverageValues.head._2 < elapsed - (interval)) {
        movingAverageValues = movingAverageValues.tail
      }

      if (elapsed == movingAverageValues.head._2) {
        0.0
      } else {
        (count - movingAverageValues.head._1) / (elapsed - movingAverageValues.head._2) * 1000.0
      }
    }
  }

  def elapsed = {
    val elapsedTime = System.currentTimeMillis - startTime;
    if (0 == elapsedTime) 1l else elapsedTime
  }

  def avgRate = count / elapsed * 1000.0

  def getLastAvgTime = {

    val curTime = System.currentTimeMillis

    if (curTime - 1000 > lastAvgTime) {
      lastAvgTime = curTime
    }

    lastAvgTime
  }
}

class StatusInfo(val stage: Int,
                 val stageDescription: String,
                 val itemCountStatus: Seq[ItemCountStatus]) {

  val startTime = System.currentTimeMillis

  val MILLI_PER_SEC = 1000l

  def formatTime(time: Long) = {
    "%02d:%02d:%02d".format(
      time / (MILLI_PER_SEC * 60 * 60),
      (time / (MILLI_PER_SEC * 60)) % 60,
      (time / MILLI_PER_SEC) % 60)
  }

  def elapsed = System.currentTimeMillis - startTime

  def elapsedString = formatTime(System.currentTimeMillis - startTime)

  def getItemCountStatus(name: String) = itemCountStatus.find(_.name.equals(name))

  override def toString = {

    val stringBuilder = new StringBuilder()

    itemCountStatus.foreach((itemCountStatus) => {

      stringBuilder.append("%,.3f %s %s                                    \n".format(itemCountStatus.countInUnit.toDouble, itemCountStatus.displayUnit.name, itemCountStatus.name))

      if(itemCountStatus.showAverage) {
        stringBuilder.append("%,.3f %s %s/sec (cumulative average)            \n".format(itemCountStatus.avgRate / itemCountStatus.displayUnit.unit.toDouble, itemCountStatus.displayUnit.name, itemCountStatus.name))
      }

      itemCountStatus.movingAverages.foreach((movingAverage) =>

        stringBuilder.append("%,.3f %s %s/sec %s        \n".format(movingAverage.latestMovingAvg / itemCountStatus.displayUnit.unit.toDouble, itemCountStatus.displayUnit.name, itemCountStatus.name, movingAverage.name))
      )
    })
    ////      putString("%d %s                                          ".format(itemCount, itemDesc))
    ////      putString("%.3fM %s/sec (10 second moving average)        ".format(shortItemMovingAvg / ONE_MILLION, itemDesc))
    //      //putString("%2.2f%% complete (approx.)                     ".format(lines.toDouble / total * 100))
    //      //putString("%s time remaining (approx.)                    ".format(formatTime(((total - lines) / longMovingAvg * 1000).toLong)))

    stringBuilder.toString
  }

}

class StatusConsole(private val screenMessage: String, private val displayInterval: Long = 1000, private val column: Int = 10) {

  var lastDisplayTime = System.currentTimeMillis

  var row = 2
  val terminal = TerminalFacade.createTerminal(Charset.forName("UTF8"))
  terminal.enterPrivateMode
  clear
  terminal.setCursorVisible(false)

  def putText(text: String) = {

    text.split("\\n").foreach(putString(_))
  }

  def putString(str: String) = {
    terminal.moveCursor(column, row)
    row += 1
    str.foreach(terminal.putCharacter(_))
  }

  def clear = {
    terminal.clearScreen
    row = 1
    putString(screenMessage)
  }

  def displayProgress(statusInfo: StatusInfo): Unit = {

    if ((System.currentTimeMillis - displayInterval) <= lastDisplayTime)
      return

    lastDisplayTime = System.currentTimeMillis

    //logStatus(statusInfo.startTime, lines)

    row = statusInfo.stage * 4 - 2
    putString("stage %d (%s)...                               ".format(statusInfo.stage, statusInfo.stageDescription))
    putString("%s elapsed                                     ".format(statusInfo.elapsedString))

    putText(statusInfo.toString)
  }

  def displayDone(statusInfo: StatusInfo) = {

    row = statusInfo.stage * 4 - 2
    putString("stage %d (%s) complete. elapsed: %s                                   ".format(statusInfo.stage, statusInfo.stageDescription, statusInfo.elapsedString))

    statusInfo.itemCountStatus.foreach((itemCountStatus) => {

      putString("%d %s processed,                                                      ".format(itemCountStatus.count, itemCountStatus.name))
      //putString("%.3f %s %s/sec (average); %.3fM %s/sec (average)                       ".format(itemCountStatus.avgRate / ONE_MILLION, itemCountStatus.displayUnit.name, itemCountStatus.name))
      putString("%,.3f %s %s/sec (average);                                             ".format(itemCountStatus.avgRate, itemCountStatus.displayUnit.name, itemCountStatus.name))
    })

    putString("                                                                      ")
  }


  def checkForExit = {
    val key = terminal.readInput()
    if (key != null && key.isCtrlPressed && key.getCharacter == 'c') {
      terminal.exitPrivateMode()
      System.exit(0)
    }
  }

  //  def logStatus(processStartTime: Long, rdfLineCount: Long) = {
  //    val curTime = System.currentTimeMillis
  //    checkForExit
  //    if (rdfLineCount % (ONE_MILLION * 10L) == 0) {
  //      // logger.info(": " + rdfLineCount / 1000000 + "M tripleString lines processed" +
  //      //   "; last 10M: " + formatTime(curTime - lastTime) +
  //      //   "; process elapsed: " + formatTime(curTime - processStartTime))
  //      //lastTime = curTime
  //    }
  //  }


}
