
var onloadCallbacks = [];
function addOnLoadCallback(callback) {
    onloadCallbacks.push(callback);
}
window.onload = () => {
    onloadCallbacks.forEach(callback => callback());
};