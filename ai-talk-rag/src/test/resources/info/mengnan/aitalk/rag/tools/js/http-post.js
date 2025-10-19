function execute(params) {
    const url = 'https://jsonplaceholder.typicode.com/posts';
    const payload = JSON.stringify({
        title: params.title,
        body: params.body,
        userId: parseInt(params.userId)
    });

    const response = http.post(url, payload);
    return response;
}