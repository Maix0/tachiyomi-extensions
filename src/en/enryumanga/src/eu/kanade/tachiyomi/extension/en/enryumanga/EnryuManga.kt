package eu.kanade.tachiyomi.extension.en.enryumanga

import android.webkit.CookieManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Observable

class EnryuManga : ParsedHttpSource() {

    override val name: String = "EnryuManga"

    override val baseUrl: String = "https://enryumanga.com"

    override val lang: String = "en"

    override val supportsLatest: Boolean = false


    private val base: HttpUrl = HttpUrl.parse(BaseUrl);

    private fun extractProxiedImage(link: String): String? {
        val resolved: HttpUrl = base.resolve(link);
        return resolved.queryParameter("url")?.run({url -> URLDecoder.decode(url, "UTF-8")});
    }

    override fun fetchPopularManga(page: Int): Observable<MangaPage> {
        var response = GET("${baseUrl}/list").asJsoup();
        return MangasPage(reponse.selectFirst("html body div div.flex.justify-center.items-center.flex-wrap")?.children().asList().map(
            {elem -> {
                var img_url = extractProxiedImage(elem.selectFirst("div > div > a > img").attributes().get("src"));
                var url = elem.selectFirst("div > div > a").attributes().get("href").replace(Regex.fromLiteral("/novels/"), "/mangas/");
                var name = elem.selectFirst("div > div > h2.card-title").ownText();

                SManga.create().apply({
                    it.url = url;
                    it.name = title;
                    it.thumbnail_url = img_url;
                })
            }
        }).asList(), false);
    }

    override fun getMangaUrl(manga: SManga): String {
        return manga.url;
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        var details = GET(manga.url).asJsoup().selectFirst(".hero-content");
        manga.title = details.select("div > h1").ownText();
        manga.description = details.select("div > p").ownText();
        return manga;
    }

    private fun parseDate(dateStr: String): Long {
    return runCatching { DATE_FORMATTER.parse(dateStr)?.time }
        .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("MM dd YYYY", Locale.ENGLISH)
        }
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        return GET(manga.url).asJsoup().selectFirst("body > div > div.flex.justify-center.flex-wrap").children().asList()
        .map({ elem ->
            return SChapter().apply({ it ->
                it.url = elem.selectFirst("a").attributes().get("href");
                it.title = elem.selectFirst(".card-title").ownText();
                it.date = parseDate(elem.selectFirst(".card-body > p").ownText());
            }
        }).asList();
    }

    override fun getChapterUrl(chapter: SChapter): String {
        return chapter.url;
    }

    override fun getPageList(chapter: SChapter): Observable<List<Page>> {
        val index = 0;
        return Observable(GET(chapter.url).selectFirst(".flex.flex-col.items-center.mx-auto").children.asList()
        .map({img_node ->
            return Page(
                index++,
                chapter.url,
                extractProxiedImage(img_node.selectFirst("img").attributes().get("src"))
            );
        }).asList());
    }
}
