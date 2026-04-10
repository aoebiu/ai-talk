function execute(params) {
    var url = params.url || 'https://jsonplaceholder.typicode.com/posts/1';
    var headers = {
        "Authorization": params.authToken || "",
        "Accept": "application/json"
    };

    var response = http.get(url, headers);
    return response;
}
