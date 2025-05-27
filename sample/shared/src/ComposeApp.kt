import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.facebook.csslayout.CSSFlexDirection
import com.facebook.csslayout.CSSLayout
import com.facebook.csslayout.CSSLayoutContext
import com.facebook.csslayout.CSSNode
import com.sumygg.kansha.elements.Text
import com.sumygg.kansha.elements.View
import com.sumygg.kansha.kansha
import kotlinx.coroutines.launch

@Composable
fun ComposeApp() {
    var url by remember { mutableStateOf("https://www.google.com/") }
    var htmlContent by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                UrlInputField(url) { url = it }
                Spacer(modifier = Modifier.size(8.dp))
                LoadButton(url) { htmlContent = it }
                Spacer(modifier = Modifier.size(8.dp))
                HtmlContentDisplay(htmlContent)
                CssLayout()
                SvgImage(
                    """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                <rect width="100%" height="100%" fill="blue"/>
                <circle cx="100" cy="100" r="80" fill="yellow"/>
                <text x="50" y="110" font-size="20" fill="white">Preview SVG</text>
            </svg>
        """.trimIndent()
                )
                HelloKansha()
            }
        }
    }
}

@Composable
fun UrlInputField(url: String, onUrlChange: (String) -> Unit) {
    Row {
        TextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.weight(1f),
            label = { Text("URL") }
        )
    }
}

@Composable
fun LoadButton(url: String, onLoadContent: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        onLoadContent("Loading...")
        scope.launch {
            try {
                val title = "Hello World"
                val body = "Hello Body"
                val content = "Page Title: $title\n\nPage Body: $body"
                onLoadContent(content)
            } catch (e: Exception) {
                onLoadContent("Failed to load content: ${e.message}")
            }
        }
    }) {
        Text("Load")
    }
}

@Composable
fun HtmlContentDisplay(htmlContent: String) {
    LazyColumn {
        item {
            Text(htmlContent, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun SvgImage(svg: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(svg.encodeToByteArray())
            .decoderFactory(SvgDecoder.Factory())
            .build(),
        contentDescription = "SVG Image",
        modifier = Modifier.width(200.dp).height(200.dp).background(Color.White),
    )
}

@Composable
fun CssLayout() {
    val root = CSSNode()
    root.style.flexDirection = CSSFlexDirection.COLUMN_REVERSE
    root.style.dimensions[CSSLayout.DIMENSION_WIDTH] = 1000f
    root.style.dimensions[CSSLayout.DIMENSION_HEIGHT] = 1000f
    val node1 = CSSNode()
    node1.layout.position[CSSLayout.POSITION_TOP] = 0f
    node1.layout.position[CSSLayout.POSITION_LEFT] = 0f
    node1.style.dimensions[CSSLayout.DIMENSION_WIDTH] = 500f
    node1.style.dimensions[CSSLayout.DIMENSION_HEIGHT] = 500f
    root.addChildAt(node1, 0)
    val node2 = CSSNode()
    node2.layout.position[CSSLayout.POSITION_TOP] = 500f
    node2.layout.position[CSSLayout.POSITION_LEFT] = 0f
    node2.style.dimensions[CSSLayout.DIMENSION_WIDTH] = 250f
    node2.style.dimensions[CSSLayout.DIMENSION_HEIGHT] = 250f
    root.addChildAt(node2, 1)
    val node3 = CSSNode()
    node3.layout.position[CSSLayout.POSITION_TOP] = 500f
    node3.layout.position[CSSLayout.POSITION_LEFT] = 0f
    node3.style.dimensions[CSSLayout.DIMENSION_WIDTH] = 250f
    node3.style.dimensions[CSSLayout.DIMENSION_HEIGHT] = 250f
    root.addChildAt(node3, 2)

    val layoutContext = CSSLayoutContext()
    root.calculateLayout(layoutContext)
    println("root ${root}")
    println("node1 ${node1.layoutX} ${node1.layoutY} ${node1.layoutWidth}  ${node1.layoutHeight}")
    println("node2 ${node2.layoutX} ${node2.layoutY}  ${node2.layoutWidth}  ${node2.layoutHeight}")
}

@Composable
fun HelloKansha() {
    val container = View()
    container.width = 100
    container.height = 100
    container.flexDirection = CSSFlexDirection.ROW
    container.backgroundColor = "red"
    val view1 = View()
    val view2 = View()
    container.add(view1)
    container.add(view2)
    view1.flex = 1f
    view1.height = 100
    view2.flex = 1f
    view2.height = 100
    view2.backgroundColor = "blue"
    val text1 = Text()
    text1.content = "Hello"
    text1.color = "#ffffff"
    val text2 = Text()
    text2.content = "Kansha"
    text2.color = "#ffffff"
    view1.add(text1)
    view2.add(text2)

    val svg = kansha {
        debug = true
        width = 100
        height = 100

        container
    }
    println("Hello Kansha\n${svg}")
    SvgImage(svg)
}