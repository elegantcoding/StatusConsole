import com.elegantcoding.statuscosole._
import org.scalatest._

class statusconsoleSpec extends FlatSpec with ShouldMatchers {

  //var log = Logger(LoggerFactory.getLogger("freebase2neoSpec"))

  val statusConsole = new StatusConsole();

  val COUNT = "count"
  val COUNT_THOUSAND = "count "
  val FIZZ = "fizz"
  val BUZZ = "buzz"
  val FIZZ_BUZZ = "fizz buzz"

  "statusconsole" should "be able to display fizz buzz" in {

    val statusInfo =
      new StatusInfo(1, "Single pass",
        Seq[ItemCountStatus](
          new ItemCountStatus(COUNT, Seq[MovingAverage](
            new MovingAverage("(10 second moving average)", (10 * 1000)),
            new MovingAverage("(10 min moving average)", (10 * 60 * 1000)))
          ),
          new ItemCountStatus(COUNT_THOUSAND, displayUnit = DisplayUnitThousand),
          new ItemCountStatus(FIZZ),
          new ItemCountStatus(BUZZ),
          new ItemCountStatus(FIZZ_BUZZ)
        )
      )

    for (i <- 0 to 10000000) {

      statusInfo.getItemCountStatus(COUNT).get.incCount
      statusInfo.getItemCountStatus(COUNT_THOUSAND).get.incCount

      if (0 == (i % 15)) {

        Thread.sleep(1)

        statusInfo.getItemCountStatus(FIZZ_BUZZ).get.incCount
      } else if (0 == (i % 5)) {
        statusInfo.getItemCountStatus(BUZZ).get.incCount

      } else if (0 == (i % 3)) {
        statusInfo.getItemCountStatus(FIZZ).get.incCount

      }

      statusConsole.displayProgress(statusInfo)

    }

    statusConsole.displayDone(statusInfo)
  }

}
