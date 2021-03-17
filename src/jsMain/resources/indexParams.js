
window.onload = (event) => {
    const urlParams = new URLSearchParams(window.location.search);
    const usernameParam = urlParams.get("username");
    const codeParam = urlParams.get("code");

    if(usernameParam != null) {
        document.getElementById("username-textfield").value = usernameParam;
    }
    if(codeParam != null && codeParam !== "") {
        let codeTextfield = document.getElementById("code-textfield")
        codeTextfield.value = codeParam;
        codeTextfield.style = "display: none; margin-top: 0pt !important; margin-bottom: 0pt !important;";
    }
};

