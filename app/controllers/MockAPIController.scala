package controllers

import org.slf4j.LoggerFactory
import play.Logger
import play.api.libs.json.Json
import play.api.mvc._

import java.util.concurrent.atomic.AtomicInteger
import javax.inject._
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MockAPIController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  private val ids = ListBuffer.empty[(Long, Long)]
  private val counter = new AtomicInteger(1)
  private var i = 0

  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>

    Future {
      synchronized {
        i += 1
      }
      ids.addOne(System.currentTimeMillis() -> i) //counter.addAndGet(1))
      Ok(
        Json.obj(
          "app" -> "MOCK_API",
          "payload" -> request.body.toString,
          "uuid" -> (request.id, i) //counter.get())
        )
      )

    }
  }

  def concurrentStats(): Action[AnyContent] = Action {
    _ =>
      Ok(
        Json.toJson[Map[Long, List[Long]]](
          ids
            .groupBy(_._1)
            .withFilter(t => t._2.lengthIs >= 2 && t._2.length != t._2.distinct.length)
            .map {
              case (ts, tuples) => ts -> tuples.map(_._2).toList
            }
        )
      )
  }

  def clear: Action[AnyContent] = Action { _ => counter.set(0); ids.clear(); Ok("") }

}