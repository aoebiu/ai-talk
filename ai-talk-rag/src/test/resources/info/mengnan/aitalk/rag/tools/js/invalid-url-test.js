function execute(params) {
    const url = 'http://invalid-url-that-does-not-exist-12345.com';

    const response = http.get(url);
    return response;
}