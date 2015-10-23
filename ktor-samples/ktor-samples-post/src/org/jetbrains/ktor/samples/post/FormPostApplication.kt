package org.jetbrains.ktor.samples.post

import kotlinx.html.*
import kotlinx.html.stream.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.locations.*
import org.jetbrains.ktor.routing.*

@location("/") class index()

class FormPostApplication(config: ApplicationConfig) : Application(config) {
    init {
        locations {
            get<index>() {
                response.status(HttpStatusCode.OK)
                response.contentType(ContentType.Text.Html)
                response.write {
                    appendHTML().html {
                        head {
                            title { +"POST" }
                        }
                        body {
                            p {
                                +"File upload example"
                            }
                            form("/form", encType = FormEncType.multipartFormData, method = FormMethod.post) {
                                textInput { name = "field1" }
                                fileInput { name = "file1" }
                                submitInput { value = "send"  }
                            }
                        }
                    }
                }
                ApplicationRequestStatus.Handled
            }

            post("/form") {
                val multipart = request.content.get<MultiPartData>()

                response.status(HttpStatusCode.OK)
                response.contentType(ContentType.Text.Plain)
                response.write {
                    if (!request.isMultipart()) {
                        appendln("Not a multipart request")
                    } else {
                        multipart.parts.forEach { part ->
                            when (part) {
                                is PartData.FormItem -> appendln("Form field: ${part.partName} = ${part.value}")
                                is PartData.FileItem -> appendln("File field: ${part.partName} -> ${part.originalFileName}")
                            }
                            part.dispose()
                        }
                    }
                }

                ApplicationRequestStatus.Handled
            }
        }
    }
}
