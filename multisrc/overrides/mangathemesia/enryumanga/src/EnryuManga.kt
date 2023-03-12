package eu.kanade.tachiyomi.extension.en.enryumanga

import android.util.Log
import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Locale

open class EnryuManga : MangaThemesia(
    "Enryu Manga",
    "https://enryumanga.com",
    "en",
    "/",
    SimpleDateFormat("MMM d, yyyy", Locale.US),
    "s",
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

    override fun chapterListSelector() = "#chapterlist > ul > li"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = getUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        name = element.select(".chapternum").text().ifBlank {
            element.text()
        }
        date_upload = element.selectFirst(".chapterdate")?.text().parseChapterDate()
        chapter_number = (element.attr("data-index").toFloatOrNull() ?: 0.0f)
    }

    override fun pageListParse(document: Document): List<Page> {
        Log.d("fsf", "REZREZRZERZE ${document.select("#readerarea img").size}")
        return document.select("#readerarea img")
            .filterNot { it -> it.imgAttr().isEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.imgAttr()) }
    }
}
