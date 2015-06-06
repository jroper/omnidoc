import com.typesafe.rpdocs.models.{ Index => RpIndex, Resources => RpResources, _ }
import play.api.libs.json.Json
import sbt._
import play.doc._

object ReactivePlatformDocs {

  def generateRpDocs(base: File, target: File, scaladoc: File, javadoc: File, linkParameters: Map[String, String]): Seq[(File, String)] = {

    val repo = new FilesystemRepository(base / "manual")
    val Some(index) = PageIndex.parseFrom(repo, "Play")

    val playDoc = new PlayDoc(repo, repo, Some(index), PlayDocConfig("resources", Map("PLAY_VERSION" -> ""), linkParameters, None))

    def mapped(dir: File, path: String) = dir.***.filter(!_.isDirectory) pair rebase(dir, path)

    // Get all the static resources
    val resources = mapped(base, "resources")
    val scaladocs = mapped(scaladoc, "api/scala")
    val javadocs = mapped(javadoc, "api/java")

    // Go through and render each page
    def renderPages(toc: TocTree): Seq[(File, String)] = {
      toc match {
        case TocPage(page, title) =>
          println("Rendering: " + page)
          val Some(renderedPage) = playDoc.renderPage(page)
          val out = target / page
          IO.write(out, renderedPage.html)
          Seq(out -> page)
        case Toc(_, _, nodes, _) =>
          nodes.flatMap {
            case (_, child) => renderPages(child)
          }
      }
    }
    val pages = renderPages(index.toc)

    // Generate index
    def toRpToc(toc: TocTree): TOC = {
      toc match {
        case TocPage(page, title) =>
          TOC(title, Some(page), false, None, Nil)
        case Toc(_, title, nodes, _) =>
          TOC(title, None, false, None, nodes.map {
            case (_, child) => toRpToc(child)
          })
      }
    }

    val rpToc = toRpToc(index.toc)
    // Inject Scala/Java docs
    val rpTocWithApi = rpToc.copy(children = rpToc.children ++ Seq(
      TOC("Scala API", Some("api/scala/index.html"), true, None, Nil),
      TOC("Java API", Some("api/java/index.html"), true, None, Nil)
    ))

    val rpIndex = RpIndex(Map("en" -> rpTocWithApi))
    val indexFile = target / "index.json"
    IO.write(indexFile, Json.prettyPrint(Json.toJson(rpIndex)))

    resources ++ scaladocs ++ javadocs ++ pages :+ (indexFile -> "index.json")
  }
}