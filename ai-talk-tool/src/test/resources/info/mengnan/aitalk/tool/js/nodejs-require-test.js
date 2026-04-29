function execute(params) {
    // 测试 Node.js 风格的 require 语法
    // 使用 npm 安装的 lodash 库
    const _ = require("lodash");

    var result = "Node.js require 语法测试成功\n";
    result += "已执行：const _ = require('lodash')\n";
    result += "lodash 版本：" + _.VERSION + "\n";

    // 测试 lodash 的基本功能
    var arr = [1, 2, 3, 4, 5];
    result += "_.reverse([1,2,3,4,5]) = " + JSON.stringify(_.reverse(arr)) + "\n";
    result += "_.first([1,2,3,4,5]) = " + _.first(arr) + "\n";

    return result;
}