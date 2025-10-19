function execute(params) {
    const userId = params.userId;
    const url = 'https://jsonplaceholder.typicode.com/posts?userId=' + userId;

    const responseStr = http.get(url);
    const responseObj = JSON.parse(responseStr);
    const posts = JSON.parse(responseObj.body);
    console.log(posts);
    // 解析响应并统计
    const count = Array.isArray(posts) ? posts.length : 0;
    const v  = JSON.stringify({
        userId: userId,
        postCount: count,
        message: '用户 ' + userId + ' 共有 ' + count + ' 篇帖子'
    });
    return v;
}