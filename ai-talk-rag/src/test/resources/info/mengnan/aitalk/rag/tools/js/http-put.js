function execute(params) {
    const url = 'https://jsonplaceholder.typicode.com/posts/' + params.postId;
    const payload = JSON.stringify({
        id: parseInt(params.postId),
        title: params.title,
        body: params.body,
        userId: 1
    });

    const response = http.put(url, payload);
    return response;
}