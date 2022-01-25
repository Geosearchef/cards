package api

import org.eclipse.jetty.http.HttpStatus.*
import spark.Spark.get
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.*
import java.util.regex.Pattern

object Api {

    val ASSET_PATTERN = Pattern.compile("[\\w-]+\\.(png|jpg)")
    lateinit var ASSETS_FOLDER: Path

    val ASSET_TOKEN: String
    init {
        val data = ByteArray(32)
        SecureRandom().nextBytes(data)
        ASSET_TOKEN = Base64.getUrlEncoder().encodeToString(data)
    }

    fun init() {
        get("/asset/:name") { req, res ->
            val fileName = req.params(":name")
            val matcher = ASSET_PATTERN.matcher(fileName)

            if(! matcher.matches()) {
                res.status(BAD_REQUEST_400)
                return@get "Bad request"
            }

            val token = req.queryParams("token")
            if(token != ASSET_TOKEN) {
                res.status(UNAUTHORIZED_401)
                return@get "Unauthorized"
            }

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