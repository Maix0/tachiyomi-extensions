package eu.kanade.tachiyomi.extension.en.zahard

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Locale

open class Zahard : MangaThemesia(
    "Zahard",
    "https://zahard.org",
    "en",
    "/library",
    SimpleDateFormat("MMM d, yyyy", Locale.US),
    "search",
) {

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig.replace(" ", "%20"))
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    override fun chapterListSelector() = "#chapterlist > ul > a"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = getUrlWithoutDomain(element.attr("href"))
        name = element.select(".eph-num").text().ifBlank {
            element.text()
        }
        date_upload = element.selectFirst(".chapterdate")?.text().parseChapterDate()
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("#chapter_imgs > img")
            .filterNot { it -> it.imgAttr().isEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.imgAttr()) }
    }
}
