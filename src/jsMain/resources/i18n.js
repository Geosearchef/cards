const language = navigator.language
let languageFile = (language === "de" || language === "de-DE" ? "messages-de.yaml" : "messages.yaml")
console.log("Detected locale: " + language + ", using " + languageFile)

let client = new XMLHttpRequest();
client.open("GET", "/i18n/" + languageFile);
client.send()

var i18nMessages = new Map()

addOnLoadCallback(() => {
    console.log("Running i18n")
    let elements = allElements(document.body)
    console.log("Found " + elements.length + " elements")
    console.log(elements)

    client.onreadystatechange = function(e) {
        let response = client.responseText
        if(response != null && response !== "") {
            response.split("\n").forEach((line) => {
                let parts = line.split(":")
                i18nMessages.set(parts[0], parts[1].trim())
            })
            applyI18n(elements, i18nMessages)
        }
    }
    if(client.responseText != null && client.responseText !== "") {
        client.onreadystatechange(null)
    }
})

function applyI18n(elements, messages) {
    console.log("Applying i18n")
    elements.forEach((element) => {
        if(!!element.value) {
            element.value = applyReplacement(element.value, messages)
        }
        if(!!element.placeholder) {
            element.placeholder = applyReplacement(element.placeholder, messages)
        }
        if(element.nodeName === "H3") {
            element.innerHTML = applyReplacement(element.innerHTML, messages)
        }
    })
    console.log("I18n done.")
}

function applyReplacement(string, messages) {
    // let re = /\[i18n\-(.*)\]/
    // var match = null
    // keys = []
    // while((match = re.exec(string)) !== null) {
    //     keys.push(match[1])
    // }
    Array.from(messages.keys()).forEach((key) => {
        if(messages.has(key)) {
            string = string.replace(`[${key}]`, messages.get(key))
        }
    })
    return string
}

function allElements(element) {
    var list = []
    if(element == null || !!!element.hasChildNodes || !element.hasChildNodes()) {
        return list
    }
    element.childNodes.forEach((childNode) => {
        if(childNode.nodeName !== "SCRIPT") {
            list.push(childNode)
            list = list.concat(allElements(childNode))
        }
    })
    return list
}

function i18nGet(key) {
    return i18nMessages.get(key)
}