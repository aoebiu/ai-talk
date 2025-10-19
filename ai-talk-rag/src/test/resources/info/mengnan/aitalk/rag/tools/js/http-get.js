function execute(params) {
    const url = 'https://jsonplaceholder.typicode.com/users/' + params.userId;
    const response = http.get(url);
    return response;
}