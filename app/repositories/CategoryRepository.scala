package me.shoma.play_cms.repositories

import javax.inject.Inject

import me.shoma.play_cms.models.Category
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CategoryRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends DBTableDefinitions {

  import profile.api._

  def list: Future[List[Category]] = {

    val action = slickCategories.sortBy(_.name.asc).to[List].result

    db.run(action).map { resultOption =>
      resultOption.map {
        case (category) =>
          Category(
            Some(category.id),
            category.name
          )
      }
    }
  }

  def findByPost(postId: Long) = {
    PostCategories.filter(_.postId === postId)
      .joinLeft(slickCategories).on(_.categoryId === _.id)
      .to[List].result
  }

  def findByPost(postIds: Seq[Long]) = {
    PostCategories.filter(_.postId.inSet(postIds))
      .joinLeft(slickCategories)
      .on(_.categoryId === _.id)
      .to[List].result
  }

  def insertOrUpdate(categories: Seq[Category]) = {
    DBIO.sequence(categories.map { current =>
      slickCategories.filter(_.name === current.name).result.headOption.flatMap {
        case Some(category) => DBIO.successful(category)
        case None => slickCategories.returning(slickCategories) += DBCategory(0, current.name)
      }
    })
  }

  case class DBPostCategory(postId: Long, categoryId: Long)

  class PostCategory(tag: Tag) extends Table[DBPostCategory](tag, "post_category") {

    def postId = column[Long]("post_id")
    def categoryId = column[Long]("category_id")

    def * = (postId, categoryId) <> (DBPostCategory.tupled, DBPostCategory.unapply _)
  }

  val PostCategories = TableQuery[PostCategory]
}
