package com.example.newsmcp

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import java.net.URLEncoder

@Serializable
data class NewsItem(
    val link: String,
    val title: String,
    val source: String,
    val og: String? = null,
    val source_icon: String? = null
)

fun main() = runBlocking {
    KotlinLoggingConfiguration.logStartupMessage = false
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    val server = Server(
        serverInfo = Implementation(
            name = "news-mcp",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    // Tool 1: Get Top News
    server.addTool(
        name = "get_top_news",
        description = "Get top news headlines from various categories",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("category", buildJsonObject {
                    put("type", "string")
                    put("description", "The category of news (Business, Entertainment, Health, Science, Sports, Technology, World)")
                    put("enum", buildJsonArray {
                        add("Business")
                        add("Entertainment")
                        add("Health")
                        add("Science")
                        add("Sports")
                        add("Technology")
                        add("World")
                    })
                })
            },
            required = emptyList()
        )
    ) { request ->
        val category = request.params.arguments?.get("category")?.jsonPrimitive?.content ?: "World"
        
        try {
            val response: Map<String, List<NewsItem>> = client.get("https://ok.surf/api/v1/cors/news-feed").body()
            val news = response[category] ?: emptyList()
            
            val text = news.joinToString("\n\n") { item ->
                "${item.title}\nSource: ${item.source}\nLink: ${item.link}"
            }

            CallToolResult(
                content = listOf(TextContent(text = if (text.isEmpty()) "No news found for category $category" else text))
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Error fetching news: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool 2: Search News
    server.addTool(
        name = "search_news",
        description = "Search for news articles by keyword",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", "string")
                    put("description", "The search keyword or phrase")
                })
            },
            required = listOf("query")
        )
    ) { request ->
        val query = request.params.arguments?.get("query")?.jsonPrimitive?.content ?: ""
        if (query.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "Search query cannot be empty")),
                isError = true
            )
        }
        
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://news.google.com/rss/search?q=$encodedQuery&hl=en-US&gl=US&ceid=US:en"
            val responseBody = client.get(url).bodyAsText()
            
            val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            val titleRegex = Regex("<title>(.*?)</title>")
            val linkRegex = Regex("<link>(.*?)</link>")
            val sourceRegex = Regex("<source.*?>(.*?)</source>")
            
            val matches = itemRegex.findAll(responseBody).take(10)
            val results = matches.map { match ->
                val content = match.groupValues[1]
                val title = titleRegex.find(content)?.groupValues?.get(1) ?: "No Title"
                val link = linkRegex.find(content)?.groupValues?.get(1) ?: "No Link"
                val source = sourceRegex.find(content)?.groupValues?.get(1) ?: "Unknown Source"
                "$title\nSource: $source\nLink: $link"
            }.toList()

            CallToolResult(
                content = listOf(TextContent(text = if (results.isEmpty()) "No results found for '$query'" else results.joinToString("\n\n")))
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Error searching news: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool 3: Get News (Topics + Hours)
    server.addTool(
        name = "get_news",
        description = "Get news articles for specific topics within a certain time range",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("topics", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("type", "string") })
                    put("description", "A list of topics to search for")
                })
                put("hours", buildJsonObject {
                    put("type", "integer")
                    put("description", "The time range in hours (e.g., 24 for the last day)")
                    put("default", 24)
                })
            },
            required = listOf("topics")
        )
    ) { request ->
        val topics = request.params.arguments?.get("topics")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val hours = request.params.arguments?.get("hours")?.jsonPrimitive?.intOrNull ?: 24
        
        if (topics.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "Topics list cannot be empty")),
                isError = true
            )
        }
        
        try {
            val queryStr = topics.joinToString(" ")
            val encodedQuery = URLEncoder.encode("$queryStr when:${hours}h", "UTF-8")
            val url = "https://news.google.com/rss/search?q=$encodedQuery&hl=en-US&gl=US&ceid=US:en"
            val responseBody = client.get(url).bodyAsText()
            
            val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            val titleRegex = Regex("<title>(.*?)</title>")
            val linkRegex = Regex("<link>(.*?)</link>")
            val sourceRegex = Regex("<source.*?>(.*?)</source>")
            
            val matches = itemRegex.findAll(responseBody).take(15)
            val results = matches.map { match ->
                val content = match.groupValues[1]
                val title = titleRegex.find(content)?.groupValues?.get(1) ?: "No Title"
                val link = linkRegex.find(content)?.groupValues?.get(1) ?: "No Link"
                val source = sourceRegex.find(content)?.groupValues?.get(1) ?: "Unknown Source"
                "$title\nSource: $source\nLink: $link"
            }.toList()

            CallToolResult(
                content = listOf(TextContent(text = if (results.isEmpty()) "No news found for topics ${topics.joinToString(", ")} in the last $hours hours" else results.joinToString("\n\n")))
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Error fetching news: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool 4: Get News Detail
    server.addTool(
        name = "get_news_detail",
        description = "Get the full text content of a specific news article by its URL",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("url", buildJsonObject {
                    put("type", "string")
                    put("description", "The direct URL of the news article")
                })
            },
            required = listOf("url")
        )
    ) { request ->
        val url = request.params.arguments?.get("url")?.jsonPrimitive?.content ?: ""

        if (url.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "URL cannot be empty")),
                isError = true
            )
        }

        try {
            val responseBody = client.get(url).bodyAsText()
            val document = Jsoup.parse(responseBody)
            
            val title = document.title()
            val articleElements = document.select("article")
            val mainText = if (articleElements.isNotEmpty()) {
                articleElements.text()
            } else {
                document.select("p").joinToString("\n\n") { it.text() }
            }

            val formattedOutput = "Title: $title\nURL: $url\n\nContent:\n$mainText"

            CallToolResult(
                content = listOf(TextContent(
                    text = if (mainText.isBlank()) "Could not extract content from the provided URL." else formattedOutput
                ))
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Error fetching article details: ${e.message}")),
                isError = true
            )
        }
    }

    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )

    val done = CompletableDeferred<Unit>()
    server.onClose {
        done.complete(Unit)
    }

    server.createSession(transport)
    
    // Wait for the closure event
    done.await()
    
    client.close()
}
