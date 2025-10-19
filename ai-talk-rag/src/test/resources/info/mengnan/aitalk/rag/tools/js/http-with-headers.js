function execute(params) {
    const url = 'https://jsonplaceholder.typicode.com/posts/1';

    const response = http.get(url);
    return response;
}