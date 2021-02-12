package api

import org.eclipse.jetty.http.HttpStatus.*
import spark.Spark.get
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

object Api {

    val ASSET_PATTERN = Pattern.compile("\\w+\\.png")
    lateinit var ASSETS_FOLDER: Path

    fun init() {
        get("/asset/:name") { req, res ->
            val fileName = req.params(":name")
            val matcher = ASSET_PATTERN.matcher(fileName)

            if(! matcher.matches()) {
                res.status(BAD_REQUEST_400)
                return@get "Bad request"
            }

            // TODO: test null
            // TODO: verify context

            val file: Path = ASSETS_FOLDER.resolve(fileName)

            if(! Files.exists(file) || Files.isDirectory(file)) {
                res.status(NOT_FOUND_404)
                return@get "Not found"
            }

            res.type("image/png")
            return@get Files.readAllBytes(file)
        }
    }

}