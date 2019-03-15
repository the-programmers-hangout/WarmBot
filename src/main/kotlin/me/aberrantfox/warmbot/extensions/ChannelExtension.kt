package me.aberrantfox.warmbot.extensions

import kotlinx.html.*
import kotlinx.html.dom.create
import net.dv8tion.jda.core.entities.*
import org.w3c.dom.Element
import java.awt.Color
import java.io.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private fun Color.toHex() = String.format("#%02x%02x%02x", red, green, blue).toUpperCase()

fun MessageChannel.htmlString(): File {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    val html = document.create.html {
        body {
            style = "background-color: #36393F; font-family: Whitney,Helvetica Neue,Helvetica,Arial,sans-serif;"

            div {
                style = "color: #949494"

                h1 {
                    this@htmlString.name
                }
            }

            iterableHistory.reversed().forEach {
                div {
                    style = "padding-bottom: 20px; padding-tp: 20px"

                    if (it.embeds.isNotEmpty() && it.author.isBot) {
                        embedToHTML(it.embeds.first(), this)
                    } else {
                        p {
                            style = "color: #D7DAD4"
                            +it.fullContent()
                        }
                    }
                }
            }
        }
    }

    return File("config/archive.html").apply { intoStream(html, outputStream()) }
}

private fun embedToHTML(messageEmbed: MessageEmbed, div: DIV) = div.apply {
    div {
        style =
                "width: 100%;" +
                        "max-width: 500px;" +
                        "min-width: 300px;" +
                        "height: auto;" +
                        "overflow: visible;" +
                        "display: flex;" +
                        "flex-direction: column;" +
                        "border-radius: 5px;" +
                        "border-left-style: solid;" +
                        "border-left-width: 5px;" +
                        "border-left-color: ${messageEmbed.color.toHex()};" +
                        "background-color: #34363C;"

        div {
            style =
                    "padding-left: 10px;" +
                            "padding-top: 15px;" +
                            "line-height: 10px;"

            if (messageEmbed.title != null) {
                div {
                    style = "color: #F1F1F1;font-size: 15px;"
                    +messageEmbed.title
                }
            }

            if (messageEmbed.description != null)
                div {
                    style = "color: #AAA9AD;font-size: 14px;Line-Height: 20px"
                    +messageEmbed.description
                }


            if (messageEmbed.thumbnail.url != null) {
                img {
                    style = "height: 80px;" +
                            "width: 80px;" +
                            "position: relative;" +
                            "float: right;" +
                            "bottom: 8px;" +
                            "margin-right: 7px;" +
                            "border-radius: 5px;" +
                            "border-style: solid;" +
                            "border-color: transparent;"

                    src = messageEmbed.thumbnail.url
                }

            }


            messageEmbed.fields.forEach {
                div {
                    style = "color: #F1F1F1;font-size: 15px;padding-bottom: 10px;"
                    +it.name
                }

                div {
                    style = "color: #AAA9AD;font-size: 14px;padding-bottom: 10px;Line-Height: 20px"
                    +it.value
                }
            }
        }
    }
}

private fun intoStream(doc: Element, out: OutputStream) =
        with(TransformerFactory.newInstance().newTransformer()) {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            setOutputProperty(OutputKeys.METHOD, "xml")
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            transform(DOMSource(doc), StreamResult(OutputStreamWriter(out, "UTF-8")))
        }