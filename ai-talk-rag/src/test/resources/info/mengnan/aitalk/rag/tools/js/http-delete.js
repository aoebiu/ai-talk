function execute(params) {
    const url = 'https://jsonplaceholder.typicode.com/posts/' + params.postId;

    const response = http.delete(url);
    return '删除成功: ' + response;
}