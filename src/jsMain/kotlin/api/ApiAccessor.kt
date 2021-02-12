package api

import org.w3c.xhr.XMLHttpRequest

object ApiAccessor {

    fun init() {
//        exampleRequest()
    }

    fun exampleRequest() { // TODO: generify
        val request = XMLHttpRequest()
        request.open("GET", "/someRoute")
        request.addEventListener("load", {
            if (request.status >= 200 && request.status < 300) {
                console.log("Http response: " + request.responseText);
            } else {
                console.warn(request.statusText, request.responseText);
            }
        })
        request.send()
    }
}