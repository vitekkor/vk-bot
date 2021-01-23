import com.petersamokhin.vksdk.core.api.botslongpoll.VkBotsLongPollApi
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.http.paramsOf
import com.petersamokhin.vksdk.core.model.VkSettings
import com.petersamokhin.vksdk.core.model.event.MessageNew
import com.petersamokhin.vksdk.http.VkKtorHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

fun main() {
    val settings = VkBotsLongPollApi.Settings(wait = 25, maxFails = 5)

    Api.client.onMessage { event: MessageNew ->
        if (event.message.text.matches(Regex("""\/.*"""))) {
            val command = event.message.text.toLowerCase()
            when {
                command.matches(Regex("""\/((п[ёе]сик)|(dog)|(puppy))( .*)?""")) -> {
                    var url = ""
                    runBlocking {
                        launch {
                            url = Api.ktor.get<Dog>("https://dog.ceo/api/breeds/image/random").message
                        }
                    }
                    val imageAttachmentString = Api.client.uploader().uploadPhotoForMessage(
                        event.message.peerId,
                        url = url
                    )
                    Api.client.sendMessage {
                        peerId = event.message.peerId
                        attachment = imageAttachmentString
                    }.execute()
                }
                command.matches(Regex("""\/((кот(ик)?)|(cat)|(kitty))( .*)?""")) -> {
                    val imageAttachmentString = Api.client.uploader().uploadPhotoForMessage(
                        event.message.peerId,
                        url = "https://cataas.com/cat?width=960"
                    )
                    Api.client.sendMessage {
                        peerId = event.message.peerId
                        attachment = imageAttachmentString
                    }.execute()
                }
                command.matches(Regex("""\/((помощь)|(help)|)( .*)?""")) -> {
                    Api.client.sendMessage {
                        peerId = event.message.peerId
                        message =
                            "Отправь /пёсик для того, чтобы получить фотографию пёсика." +
                                    "\nОтправь /котик для того, чтобы получить фотографию котика." +
                                    "\nДля вывода этого сообщения напиши /помощь"
                    }.execute()
                }
                else -> {
                    Api.client.sendMessage {
                        peerId = event.message.peerId
                        message = "Неверная команда"
                    }.execute()
                }
            }
        }
    }

    Api.client.startLongPolling(restart = false, settings = settings)
}

object Api {
    val ktor = HttpClient(CIO) {
        this.install(JsonFeature) {
            serializer = KotlinxSerializer()
            acceptContentTypes += ContentType("application", "json+hal")
        }
        this.engine {
            requestTimeout = 30_000L
        }
    }

    private val httpClient = VkKtorHttpClient(
        coroutineContext = Dispatchers.IO,
        overrideClient = this.ktor
    )

    private val vkClientSettings = VkSettings(
        httpClient = this.httpClient,
        apiVersion = 5.122,
        defaultParams = paramsOf("lang" to "ru"),
        maxExecuteRequestsPerSecond = 3,
        backgroundDispatcher = Dispatchers.Default,
    )
    val client = VkApiClient(
        id = 157005084,
        token = "token",
        type = VkApiClient.Type.Community,
        settings = this.vkClientSettings
    )
}

@Serializable
data class Dog(val message: String, val status: String)